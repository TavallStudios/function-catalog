package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class TavallSkillResourcePublisher {
    static final String MANIFEST_URI = "skills://tavall/manifest.json";
    static final String SKILL_URI_TEMPLATE = "skills://tavall/{skillId}/SKILL.md";
    static final String MANIFEST_MEDIA_TYPE = "application/json; charset=utf-8";
    static final String SKILL_MEDIA_TYPE = "text/markdown; charset=utf-8";

    private static final String SCHEMA_VERSION = "tavall-skill-bundle:v1";
    private static final Pattern SKILL_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private static final Pattern SHA_256 = Pattern.compile("[a-f0-9]{64}");
    private static final long MAX_MANIFEST_BYTES = 4L * 1024L * 1024L;
    private static final long MAX_SKILL_BYTES = 1024L * 1024L;

    private final ObjectMapper objectMapper;
    private final Path root;

    TavallSkillResourcePublisher(ObjectMapper objectMapper, Path root) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
        Path normalizedRoot = requireValue(root, "root").toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Skill resource root must be an existing directory: " + normalizedRoot);
        }
        try {
            this.root = normalizedRoot.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to resolve skill resource root: " + normalizedRoot, exception);
        }
    }

    McpServerFeatures.SyncResourceSpecification manifestResourceSpecification() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri(MANIFEST_URI)
                .name("Tavall skill manifest")
                .description("Current Tavall MCP skill bundle manifest with canonical source paths and SHA-256 digests.")
                .mimeType(MANIFEST_MEDIA_TYPE)
                .build();
        return new McpServerFeatures.SyncResourceSpecification(
                resource,
                (exchange, request) -> manifestReadResult()
        );
    }

    McpServerFeatures.SyncResourceTemplateSpecification skillResourceTemplateSpecification() {
        McpSchema.ResourceTemplate template = McpSchema.ResourceTemplate.builder()
                .uriTemplate(SKILL_URI_TEMPLATE)
                .name("Tavall skill")
                .description("Read one current Tavall SKILL.md body declared by the Tavall skill manifest.")
                .mimeType(SKILL_MEDIA_TYPE)
                .build();
        return new McpServerFeatures.SyncResourceTemplateSpecification(
                template,
                (exchange, request) -> skillReadResult(request.uri())
        );
    }

    McpSchema.ReadResourceResult manifestReadResult() {
        String text = readManifestText();
        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(MANIFEST_URI, MANIFEST_MEDIA_TYPE, text)
        ));
    }

    McpSchema.ReadResourceResult skillReadResult(String uri) {
        String text = readSkillText(uri);
        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(uri, SKILL_MEDIA_TYPE, text)
        ));
    }

    String readManifestText() {
        Path manifestPath = root.resolve("manifest.json").normalize();
        String manifestText = readBoundedFile(manifestPath, MAX_MANIFEST_BYTES, "skill manifest");
        parseManifest(manifestText);
        return manifestText;
    }

    String readSkillText(String uri) {
        SkillManifest manifest = parseManifest(readBoundedFile(
                root.resolve("manifest.json").normalize(),
                MAX_MANIFEST_BYTES,
                "skill manifest"
        ));
        SkillEntry entry = manifest.byUri().get(requireText(uri, "uri"));
        if (entry == null) {
            throw new IllegalArgumentException("Skill URI is not declared by the current manifest: " + uri);
        }

        Path skillPath = root.resolve("skills").resolve(entry.id()).resolve("SKILL.md").normalize();
        if (!skillPath.startsWith(root)) {
            throw new IllegalArgumentException("Skill path escapes the configured resource root: " + entry.id());
        }
        if (!Files.isRegularFile(skillPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Skill body is missing or is not a regular file: " + entry.id());
        }

        try {
            Path realSkillPath = skillPath.toRealPath();
            if (!realSkillPath.startsWith(root)) {
                throw new IllegalArgumentException("Skill body resolves outside the configured resource root: " + entry.id());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve skill body: " + entry.id(), exception);
        }

        byte[] body = readBoundedBytes(skillPath, MAX_SKILL_BYTES, "skill body " + entry.id());
        if (body.length != entry.size()) {
            throw new IllegalStateException("Skill body size does not match the manifest for " + entry.id());
        }
        String digest = sha256(body);
        if (!digest.equals(entry.sha256())) {
            throw new IllegalStateException("Skill body digest does not match the manifest for " + entry.id());
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private SkillManifest parseManifest(String manifestText) {
        final JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(manifestText);
        } catch (IOException exception) {
            throw new IllegalStateException("Skill manifest is not valid JSON", exception);
        }
        if (!SCHEMA_VERSION.equals(rootNode.path("schemaVersion").asText())) {
            throw new IllegalStateException("Unsupported skill manifest schema: " + rootNode.path("schemaVersion").asText());
        }
        if (!MANIFEST_URI.equals(rootNode.path("manifestUri").asText())) {
            throw new IllegalStateException("Skill manifest URI must be " + MANIFEST_URI);
        }

        JsonNode skills = rootNode.path("skills");
        if (!skills.isArray()) {
            throw new IllegalStateException("Skill manifest must contain a skills array");
        }

        Map<String, SkillEntry> byUri = new HashMap<>();
        Map<String, SkillEntry> byId = new HashMap<>();
        for (JsonNode node : skills) {
            String id = node.path("id").asText();
            String uri = node.path("uri").asText();
            String sha256 = node.path("sha256").asText();
            long size = node.path("size").asLong(-1L);

            if (!SKILL_ID.matcher(id).matches()) {
                throw new IllegalStateException("Invalid skill id in manifest: " + id);
            }
            String expectedUri = "skills://tavall/" + id + "/SKILL.md";
            if (!expectedUri.equals(uri)) {
                throw new IllegalStateException("Skill URI does not match its id: " + id);
            }
            if (!SHA_256.matcher(sha256).matches()) {
                throw new IllegalStateException("Invalid SHA-256 digest for skill: " + id);
            }
            if (size < 0L || size > MAX_SKILL_BYTES) {
                throw new IllegalStateException("Invalid skill size for " + id + ": " + size);
            }

            SkillEntry entry = new SkillEntry(id, uri, sha256, size);
            if (byUri.put(uri, entry) != null || byId.put(id, entry) != null) {
                throw new IllegalStateException("Duplicate skill identity or URI in manifest: " + id);
            }
        }
        return new SkillManifest(Map.copyOf(byUri));
    }

    private String readBoundedFile(Path path, long maximumBytes, String description) {
        return new String(readBoundedBytes(path, maximumBytes, description), StandardCharsets.UTF_8);
    }

    private byte[] readBoundedBytes(Path path, long maximumBytes, String description) {
        try {
            long size = Files.size(path);
            if (size > maximumBytes) {
                throw new IllegalStateException(description + " exceeds the maximum size of " + maximumBytes + " bytes");
            }
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + description + ": " + path, exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    private record SkillEntry(String id, String uri, String sha256, long size) {
    }

    private record SkillManifest(Map<String, SkillEntry> byUri) {
    }
}

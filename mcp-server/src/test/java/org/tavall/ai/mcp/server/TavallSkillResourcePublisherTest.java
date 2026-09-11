package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TavallSkillResourcePublisherTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldReadManifestAndVerifiedSkillBody() throws Exception {
        Path root = Files.createTempDirectory("tavall-skill-resources");
        writeBundle(root, "tavall-ai", "---\nname: tavall-ai\n---\nCurrent body.\n");
        TavallSkillResourcePublisher publisher = new TavallSkillResourcePublisher(objectMapper, root);

        assertTrue(publisher.readManifestText().contains("skills://tavall/tavall-ai/SKILL.md"));
        assertEquals(
                "---\nname: tavall-ai\n---\nCurrent body.\n",
                publisher.readSkillText("skills://tavall/tavall-ai/SKILL.md")
        );
    }

    @Test
    void shouldObserveBundleUpdatesWithoutRecreatingPublisher() throws Exception {
        Path root = Files.createTempDirectory("tavall-skill-live-update");
        writeBundle(root, "tavall-ai", "version one\n");
        TavallSkillResourcePublisher publisher = new TavallSkillResourcePublisher(objectMapper, root);
        assertEquals("version one\n", publisher.readSkillText("skills://tavall/tavall-ai/SKILL.md"));

        writeBundle(root, "tavall-ai", "version two\n");

        assertEquals("version two\n", publisher.readSkillText("skills://tavall/tavall-ai/SKILL.md"));
    }

    @Test
    void shouldFailClosedWhenBodyDoesNotMatchManifestDigest() throws Exception {
        Path root = Files.createTempDirectory("tavall-skill-stale-body");
        writeBundle(root, "tavall-ai", "expected\n");
        Files.writeString(root.resolve("skills/tavall-ai/SKILL.md"), "tampered\n");
        TavallSkillResourcePublisher publisher = new TavallSkillResourcePublisher(objectMapper, root);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publisher.readSkillText("skills://tavall/tavall-ai/SKILL.md")
        );
        assertTrue(exception.getMessage().contains("does not match the manifest"));
    }

    @Test
    void shouldRejectUndeclaredAndUnsafeSkillIdentifiers() throws Exception {
        Path root = Files.createTempDirectory("tavall-skill-unsafe");
        writeBundle(root, "tavall-ai", "safe\n");
        TavallSkillResourcePublisher publisher = new TavallSkillResourcePublisher(objectMapper, root);

        assertThrows(
                IllegalArgumentException.class,
                () -> publisher.readSkillText("skills://tavall/not-declared/SKILL.md")
        );

        byte[] content = "unsafe\n".getBytes(StandardCharsets.UTF_8);
        Map<String, Object> unsafeManifest = Map.of(
                "schemaVersion", "tavall-skill-bundle:v1",
                "manifestUri", TavallSkillResourcePublisher.MANIFEST_URI,
                "skills", List.of(Map.of(
                        "id", "../escape",
                        "uri", "skills://tavall/../escape/SKILL.md",
                        "sha256", sha256(content),
                        "size", content.length
                ))
        );
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("manifest.json").toFile(), unsafeManifest);

        assertThrows(IllegalStateException.class, publisher::readManifestText);
    }

    @Test
    void shouldExposeManifestAndParameterizedSkillResourceSpecifications() throws Exception {
        Path root = Files.createTempDirectory("tavall-skill-specs");
        writeBundle(root, "tavall-ai", "body\n");
        TavallSkillResourcePublisher publisher = new TavallSkillResourcePublisher(objectMapper, root);

        assertEquals(
                TavallSkillResourcePublisher.MANIFEST_URI,
                publisher.manifestResourceSpecification().resource().uri()
        );
        assertEquals(
                TavallSkillResourcePublisher.SKILL_URI_TEMPLATE,
                publisher.skillResourceTemplateSpecification().resourceTemplate().uriTemplate()
        );
    }

    private void writeBundle(Path root, String skillId, String body) throws Exception {
        Path skillPath = root.resolve("skills").resolve(skillId).resolve("SKILL.md");
        Files.createDirectories(skillPath.getParent());
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        Files.write(skillPath, content);

        Map<String, Object> manifest = Map.of(
                "schemaVersion", "tavall-skill-bundle:v1",
                "sourceRepository", "TavallStudios/tavall-ai",
                "bundleVersion", "test",
                "digestAlgorithm", "sha256",
                "bundleDigest", sha256((skillId + ":" + sha256(content)).getBytes(StandardCharsets.UTF_8)),
                "manifestUri", TavallSkillResourcePublisher.MANIFEST_URI,
                "skills", List.of(Map.of(
                        "id", skillId,
                        "uri", "skills://tavall/" + skillId + "/SKILL.md",
                        "mediaType", TavallSkillResourcePublisher.SKILL_MEDIA_TYPE,
                        "sourcePath", "plugins/test/SKILL.md",
                        "sha256", sha256(content),
                        "size", content.length
                ))
        );
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("manifest.json").toFile(), manifest);
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}

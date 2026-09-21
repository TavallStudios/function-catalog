package org.tavall.ai.staging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and rewrites the machine-readable tavall-staging:v1 block while preserving surrounding PR prose. */
public final class StagingMetadataDocument {
    public static final String MARKER = "<!-- tavall-staging:v1 -->";
    private static final Pattern MARKER_LINE = Pattern.compile(
            "(?m)^" + Pattern.quote(MARKER) + "(?:\\r?\\n|$)"
    );
    private static final Pattern FIELD_LINE = Pattern.compile(
            "([A-Za-z][A-Za-z0-9]*):[ \\t]*([^\\r\\n]*)(?:\\r?\\n|$)"
    );
    private static final Set<String> KNOWN_FIELDS = Set.of(
            "Type",
            "Lifecycle",
            "State",
            "Branch",
            "Parent",
            "Promotion",
            "ChildMergeTarget",
            "RuntimeId",
            "RuntimeStack",
            "FanInMode",
            "RuntimeFlags",
            "ArchitectureProfile",
            "ArchitectureCheck"
    );

    private final String body;
    private final Optional<StagingMetadata> metadata;
    private final int blockStart;
    private final int blockEnd;
    private final boolean malformed;

    private StagingMetadataDocument(
            String body,
            Optional<StagingMetadata> metadata,
            int blockStart,
            int blockEnd,
            boolean malformed
    ) {
        this.body = body;
        this.metadata = metadata;
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
        this.malformed = malformed;
    }

    public static StagingMetadataDocument parse(String body) {
        String safeBody = body == null ? "" : body;
        Matcher markerMatcher = MARKER_LINE.matcher(safeBody);
        if (!markerMatcher.find()) {
            return new StagingMetadataDocument(
                    safeBody,
                    Optional.empty(),
                    -1,
                    -1,
                    safeBody.contains(MARKER)
            );
        }

        Map<String, String> fields = new LinkedHashMap<>();
        int cursor = markerMatcher.end();
        int blockEnd = cursor;
        Matcher fieldMatcher = FIELD_LINE.matcher(safeBody);
        while (cursor < safeBody.length()) {
            fieldMatcher.region(cursor, safeBody.length());
            if (!fieldMatcher.lookingAt() || !KNOWN_FIELDS.contains(fieldMatcher.group(1))) {
                break;
            }
            fields.put(fieldMatcher.group(1), fieldMatcher.group(2).trim());
            blockEnd = fieldMatcher.end();
            cursor = blockEnd;
        }

        Set<String> required = Set.of(
                "Type",
                "State",
                "Branch",
                "Parent",
                "Promotion",
                "ChildMergeTarget"
        );
        if (!fields.keySet().containsAll(required)) {
            return new StagingMetadataDocument(safeBody, Optional.empty(), markerMatcher.start(), blockEnd, true);
        }

        try {
            StagingMetadata metadata = new StagingMetadata(
                    StagingType.valueOf(fields.get("Type")),
                    StagingState.valueOf(fields.get("State")),
                    fields.get("Branch"),
                    fields.get("Parent"),
                    fields.get("Promotion"),
                    fields.get("ChildMergeTarget"),
                    optional(fields.get("Lifecycle")),
                    optional(fields.get("RuntimeId")),
                    optional(fields.get("RuntimeStack")),
                    optional(fields.get("FanInMode")),
                    optional(fields.get("RuntimeFlags")),
                    optional(fields.get("ArchitectureProfile")),
                    optional(fields.get("ArchitectureCheck"))
            );
            return new StagingMetadataDocument(
                    safeBody,
                    Optional.of(metadata),
                    markerMatcher.start(),
                    blockEnd,
                    false
            );
        } catch (RuntimeException exception) {
            return new StagingMetadataDocument(safeBody, Optional.empty(), markerMatcher.start(), blockEnd, true);
        }
    }

    public Optional<StagingMetadata> metadata() {
        return metadata;
    }

    public boolean malformed() {
        return malformed;
    }

    public String withState(StagingState state) {
        StagingMetadata current = metadata.orElseThrow(() -> new IllegalStateException("No valid tavall-staging:v1 metadata block"));
        return replace(new StagingMetadata(
                current.type(),
                state,
                current.branch(),
                current.parent(),
                current.promotion(),
                current.childMergeTarget(),
                current.lifecycle(),
                current.runtimeId(),
                current.runtimeStack(),
                current.fanInMode(),
                current.runtimeFlags(),
                current.architectureProfile(),
                current.architectureCheck()
        ));
    }

    public String replace(StagingMetadata replacement) {
        String rendered = render(replacement);
        if (blockStart < 0) {
            return rendered + (body.isBlank() ? "" : "\n\n" + body);
        }
        return body.substring(0, blockStart) + rendered + body.substring(blockEnd);
    }

    public static String render(StagingMetadata metadata) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(MARKER);
        lines.add("Type: " + metadata.type().name());
        add(lines, "Lifecycle", metadata.lifecycle());
        lines.add("State: " + metadata.state().name());
        lines.add("Branch: " + metadata.branch());
        lines.add("Parent: " + metadata.parent());
        lines.add("Promotion: " + metadata.promotion());
        lines.add("ChildMergeTarget: " + metadata.childMergeTarget());
        add(lines, "RuntimeId", metadata.runtimeId());
        add(lines, "RuntimeStack", metadata.runtimeStack());
        add(lines, "FanInMode", metadata.fanInMode());
        add(lines, "RuntimeFlags", metadata.runtimeFlags());
        add(lines, "ArchitectureProfile", metadata.architectureProfile());
        add(lines, "ArchitectureCheck", metadata.architectureCheck());
        return String.join("\n", lines);
    }

    private static Optional<String> optional(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private static void add(ArrayList<String> lines, String name, Optional<String> value) {
        value.ifPresent(text -> lines.add(name + ": " + text));
    }
}

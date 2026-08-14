package org.tavall.ai.staging;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and rewrites the machine-readable tavall-staging:v1 block while preserving surrounding PR prose. */
public final class StagingMetadataDocument {
    public static final String MARKER = "<!-- tavall-staging:v1 -->";
    private static final Pattern BLOCK = Pattern.compile(
            "(?m)" + Pattern.quote(MARKER) + "\\R"
                    + "Type: ([A-Z_]+)\\R"
                    + "State: ([A-Z_]+)\\R"
                    + "Branch: ([^\\r\\n]+)\\R"
                    + "Parent: ([^\\r\\n]+)\\R"
                    + "Promotion: ([^\\r\\n]+)\\R"
                    + "ChildMergeTarget: ([^\\r\\n]+)"
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
        Matcher matcher = BLOCK.matcher(safeBody);
        if (!matcher.find()) {
            return new StagingMetadataDocument(
                    safeBody,
                    Optional.empty(),
                    -1,
                    -1,
                    safeBody.contains(MARKER)
            );
        }
        try {
            StagingMetadata metadata = new StagingMetadata(
                    StagingType.valueOf(matcher.group(1)),
                    StagingState.valueOf(matcher.group(2)),
                    matcher.group(3),
                    matcher.group(4),
                    matcher.group(5),
                    matcher.group(6)
            );
            return new StagingMetadataDocument(safeBody, Optional.of(metadata), matcher.start(), matcher.end(), false);
        } catch (RuntimeException exception) {
            return new StagingMetadataDocument(safeBody, Optional.empty(), matcher.start(), matcher.end(), true);
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
                current.type(), state, current.branch(), current.parent(), current.promotion(), current.childMergeTarget()
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
        return String.join("\n",
                MARKER,
                "Type: " + metadata.type().name(),
                "State: " + metadata.state().name(),
                "Branch: " + metadata.branch(),
                "Parent: " + metadata.parent(),
                "Promotion: " + metadata.promotion(),
                "ChildMergeTarget: " + metadata.childMergeTarget()
        );
    }
}

package org.tavall.ai.core.schema;

import java.util.Map;
import java.util.Set;

public record SchemaCollectionPayload(Map<String, SchemaRecordPayload> payloadByKey, Set<Integer> ids) {
}
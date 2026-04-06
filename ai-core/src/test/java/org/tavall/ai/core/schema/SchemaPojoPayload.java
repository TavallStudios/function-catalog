package org.tavall.ai.core.schema;

import java.util.List;

public final class SchemaPojoPayload {
    private final SchemaStatus status;
    private final List<SchemaRecordPayload> items;

    public SchemaPojoPayload(SchemaStatus status, List<SchemaRecordPayload> items) {
        this.status = status;
        this.items = items;
    }

    public SchemaStatus getStatus() {
        return status;
    }

    public List<SchemaRecordPayload> getItems() {
        return items;
    }
}
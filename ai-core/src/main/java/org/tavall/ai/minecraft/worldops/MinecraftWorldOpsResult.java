package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftWorldOpsResult(
        boolean success,
        MinecraftWorldOpsOperationId operationId,
        MinecraftWorldOpsOperationKind operationKind,
        String detail
) {
    public MinecraftWorldOpsResult {
        operationId = Objects.requireNonNull(operationId, "operationId");
        operationKind = Objects.requireNonNull(operationKind, "operationKind");
        detail = Objects.requireNonNullElse(detail, "");
    }

    public static MinecraftWorldOpsResult succeeded(
            MinecraftWorldOpsOperationId operationId,
            MinecraftWorldOpsOperationKind operationKind,
            String detail
    ) {
        return new MinecraftWorldOpsResult(true, operationId, operationKind, detail);
    }
}

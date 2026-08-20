package org.tavall.ai.minecraft.worldops;

public enum MinecraftClipboardRotation {
    CLOCKWISE_90(90),
    CLOCKWISE_180(180),
    CLOCKWISE_270(270);

    private final int degrees;

    MinecraftClipboardRotation(int degrees) {
        this.degrees = degrees;
    }

    public int degrees() {
        return degrees;
    }
}
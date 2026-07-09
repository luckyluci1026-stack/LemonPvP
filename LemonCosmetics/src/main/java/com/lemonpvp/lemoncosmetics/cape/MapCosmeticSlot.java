package com.lemonpvp.lemoncosmetics.cape;

/**
 * An attachment point for a map-rendered cosmetic (cape on the back, bandana on
 * the head). Each slot carries its own upload folder, config prefix and default
 * placement transform. All numeric defaults can be overridden live in config
 * under {@code <configPrefix>.*} and re-applied with the cosmetic's reload
 * subcommand — the exact offset/scale/rotation is best tuned visually in-game.
 */
public enum MapCosmeticSlot {

    // folder / configPrefix, offX, offY, offZ, scaleX, scaleY, scaleZ, pitch°, yaw°, frameTicks
    CAPE("capes", "capes",
            0.0f, 0.20f, -0.30f,   // behind the back
            0.90f, 1.40f, 0.02f,   // tall thin panel
            90.0f, 180.0f, 2),

    BANDANA("bandanas", "bandanas",
            0.0f, 1.55f, 0.18f,    // around the head, slightly forward
            0.55f, 0.30f, 0.02f,   // wide thin band
            0.0f, 0.0f, 2);

    public final String folder;
    public final String configPrefix;
    public final float offX, offY, offZ;
    public final float scaleX, scaleY, scaleZ;
    public final float pitch, yaw;
    public final int frameTicks;

    MapCosmeticSlot(String folder, String configPrefix,
                    float offX, float offY, float offZ,
                    float scaleX, float scaleY, float scaleZ,
                    float pitch, float yaw, int frameTicks) {
        this.folder = folder;
        this.configPrefix = configPrefix;
        this.offX = offX; this.offY = offY; this.offZ = offZ;
        this.scaleX = scaleX; this.scaleY = scaleY; this.scaleZ = scaleZ;
        this.pitch = pitch; this.yaw = yaw;
        this.frameTicks = frameTicks;
    }
}

package de.lemonpvp.flfac.check;

/**
 * Every cheat category FLFAC can detect. The {@code configKey} maps to the
 * section under {@code checks:} in config.yml.
 */
public enum CheckType {

    // Combat
    REACH("Reach", Category.COMBAT),
    HITBOX("Hitbox", Category.COMBAT),
    KILLAURA("KillAura", Category.COMBAT),
    AUTOCLICKER("AutoClicker", Category.COMBAT),
    VELOCITY("Velocity", Category.COMBAT),

    // Movement
    SPEED("Speed", Category.MOVEMENT),
    FLY("Fly", Category.MOVEMENT),
    NOFALL("NoFall", Category.MOVEMENT),
    JESUS("Jesus", Category.MOVEMENT),
    TIMER("Timer", Category.MOVEMENT),

    // World
    SCAFFOLD("Scaffold", Category.WORLD),
    NUKER("Nuker", Category.WORLD),
    FASTBREAK("FastBreak", Category.WORLD),

    // Player / packets
    BADPACKETS("BadPackets", Category.PLAYER);

    private final String displayName;
    private final Category category;

    CheckType(String displayName, Category category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Category getCategory() {
        return category;
    }

    /** Config section key under {@code checks:}. */
    public String getConfigKey() {
        return name().toLowerCase();
    }

    public enum Category {
        COMBAT,
        MOVEMENT,
        WORLD,
        PLAYER
    }
}

package de.lemonpvp.flfac.data;

import de.lemonpvp.flfac.check.CheckType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player runtime state. Holds violation levels and the rolling buffers the
 * checks need (rotations, click timings, movement samples, ...).
 */
public final class PlayerData {

    private final UUID uuid;
    private final String name;
    private final WeakReference<Player> playerRef;

    private final Map<CheckType, Double> violations = new EnumMap<>(CheckType.class);
    private final Map<CheckType, Long> lastAlert = new EnumMap<>(CheckType.class);

    // Staff toggles
    private boolean alertsEnabled;
    private boolean verboseEnabled;

    // --- Rotation / combat ---
    private float yaw, pitch;
    private float lastYaw, lastPitch;
    private float deltaYaw, deltaPitch;
    private final Deque<Long> clickTimes = new ArrayDeque<>();
    private final Deque<Float> yawSamples = new ArrayDeque<>();
    private long lastAttackTime;

    // --- Velocity (knockback) ---
    private double pendingVelX, pendingVelY, pendingVelZ;
    private int velocityTicks;
    private boolean hasPendingVelocity;
    private long velocityGraceUntil;

    // --- Movement ---
    private Location lastLocation;
    private Location lastLastLocation;
    private long lastMoveMillis;
    private double timerBalance;
    private int airTicks;
    private int groundTicks;
    private boolean lastOnGround = true;
    private double lastDeltaY;
    private double airFallDistance;

    // --- World / blocks ---
    private long blockTickStamp;
    private int blocksThisTick;
    private long breakStartMillis;

    public PlayerData(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.playerRef = new WeakReference<>(player);
        this.lastLocation = player.getLocation();
        this.yaw = lastLocation.getYaw();
        this.pitch = lastLocation.getPitch();
    }

    public Player getPlayer() {
        return playerRef.get();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    // ----- Violations -----
    public double getViolations(CheckType type) {
        return violations.getOrDefault(type, 0.0D);
    }

    public double addViolations(CheckType type, double add, double max) {
        double value = Math.min(max, getViolations(type) + add);
        violations.put(type, value);
        return value;
    }

    public void decay(double amount) {
        if (violations.isEmpty()) {
            return;
        }
        violations.replaceAll((type, value) -> Math.max(0.0D, value - amount));
    }

    public void resetViolations() {
        violations.clear();
    }

    public double totalViolations() {
        double total = 0.0D;
        for (double v : violations.values()) {
            total += v;
        }
        return total;
    }

    public boolean isOnAlertCooldown(CheckType type, long cooldownMs, long now) {
        Long last = lastAlert.get(type);
        if (last != null && now - last < cooldownMs) {
            return true;
        }
        lastAlert.put(type, now);
        return false;
    }

    // ----- Staff toggles -----
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }
    public boolean isVerboseEnabled() { return verboseEnabled; }
    public void setVerboseEnabled(boolean verboseEnabled) { this.verboseEnabled = verboseEnabled; }

    // ----- Rotation / combat -----
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getLastYaw() { return lastYaw; }
    public float getLastPitch() { return lastPitch; }
    public float getDeltaYaw() { return deltaYaw; }
    public float getDeltaPitch() { return deltaPitch; }

    public void updateRotation(float newYaw, float newPitch) {
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.yaw = newYaw;
        this.pitch = newPitch;
        this.deltaYaw = Math.abs(de.lemonpvp.flfac.util.MathUtil.wrapDegrees(newYaw - lastYaw));
        this.deltaPitch = Math.abs(newPitch - lastPitch);
    }

    public Deque<Long> getClickTimes() { return clickTimes; }
    public Deque<Float> getYawSamples() { return yawSamples; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }

    // ----- Velocity -----
    public boolean hasPendingVelocity() { return hasPendingVelocity; }
    public int getVelocityTicks() { return velocityTicks; }
    public double getPendingVelX() { return pendingVelX; }
    public double getPendingVelY() { return pendingVelY; }
    public double getPendingVelZ() { return pendingVelZ; }

    public void setPendingVelocity(double x, double y, double z) {
        this.pendingVelX = x;
        this.pendingVelY = y;
        this.pendingVelZ = z;
        this.velocityTicks = 0;
        this.hasPendingVelocity = true;
    }

    public void tickVelocity() {
        if (hasPendingVelocity) {
            velocityTicks++;
        }
    }

    public void clearVelocity() {
        this.hasPendingVelocity = false;
        this.velocityTicks = 0;
    }

    public long getVelocityGraceUntil() { return velocityGraceUntil; }
    public void setVelocityGraceUntil(long velocityGraceUntil) { this.velocityGraceUntil = velocityGraceUntil; }
    public boolean inVelocityGrace(long now) { return now < velocityGraceUntil; }

    // ----- Movement -----
    public Location getLastLocation() { return lastLocation; }
    public Location getLastLastLocation() { return lastLastLocation; }

    public void pushLocation(Location location) {
        this.lastLastLocation = this.lastLocation;
        this.lastLocation = location;
    }

    public long getLastMoveMillis() { return lastMoveMillis; }
    public void setLastMoveMillis(long lastMoveMillis) { this.lastMoveMillis = lastMoveMillis; }
    public double getTimerBalance() { return timerBalance; }
    public void setTimerBalance(double timerBalance) { this.timerBalance = timerBalance; }
    public int getAirTicks() { return airTicks; }
    public void setAirTicks(int airTicks) { this.airTicks = airTicks; }
    public int getGroundTicks() { return groundTicks; }
    public void setGroundTicks(int groundTicks) { this.groundTicks = groundTicks; }
    public boolean isLastOnGround() { return lastOnGround; }
    public void setLastOnGround(boolean lastOnGround) { this.lastOnGround = lastOnGround; }
    public double getLastDeltaY() { return lastDeltaY; }
    public void setLastDeltaY(double lastDeltaY) { this.lastDeltaY = lastDeltaY; }
    public double getAirFallDistance() { return airFallDistance; }
    public void setAirFallDistance(double airFallDistance) { this.airFallDistance = airFallDistance; }

    // ----- World -----
    public long getBlockTickStamp() { return blockTickStamp; }
    public void setBlockTickStamp(long blockTickStamp) { this.blockTickStamp = blockTickStamp; }
    public int getBlocksThisTick() { return blocksThisTick; }
    public void setBlocksThisTick(int blocksThisTick) { this.blocksThisTick = blocksThisTick; }
    public long getBreakStartMillis() { return breakStartMillis; }
    public void setBreakStartMillis(long breakStartMillis) { this.breakStartMillis = breakStartMillis; }
}

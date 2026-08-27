package com.lemonpvp.lemoncosmetics.display;

import com.github.retrooper.packetevents.util.Vector3f;

import java.util.Locale;

/**
 * The built-in Fortnite-style dance emotes. Each emote is a pure function of
 * time: {@link #poseAt(int)} returns the armor-stand pose (all angles in
 * degrees, the unit armor-stand rotations use on the wire) for an animation
 * tick, and the driver replays it every 2 ticks. Keyframes are cheap sine
 * mixes, so they loop seamlessly and stay tunable.
 */
public enum DanceEmote {

    /** Right arm raised, waving side to side. */
    WAVE(120),
    /** The dab: face down into the elbow, other arm straight out. */
    DAB(80),
    /** Arms out, full-body 360° spins. */
    SPIN(120),
    /** The floss: straight arms swinging across the hips, hips counter-swinging. */
    FLOSS(160),
    /** The robot: stiff right-angle arm positions snapping every half second. */
    ROBOT(160);

    /** A full armor-stand pose plus a body-yaw offset, angles in degrees. */
    public record Pose(Vector3f head, Vector3f body, Vector3f leftArm, Vector3f rightArm,
                       Vector3f leftLeg, Vector3f rightLeg, float yawOffset) {}

    private final int durationTicks;

    DanceEmote(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public int durationTicks() { return durationTicks; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static DanceEmote byId(String id) {
        for (DanceEmote e : values()) if (e.id().equalsIgnoreCase(id)) return e;
        return null;
    }

    /** The pose for animation tick {@code t} (0-based, advances 2 per frame). */
    public Pose poseAt(int t) {
        return switch (this) {
            case WAVE -> new Pose(
                    v(0, 0, (float) osc(t, 20, 8)),          // head tilts with the wave
                    v(0, 0, (float) osc(t, 20, 3)),
                    v(-10, 0, -5),                            // idle left arm
                    v(-150, 0, (float) osc(t, 10, 30)),       // raised right arm sweeping
                    v(0, 0, 0), v(0, 0, 0), 0f);
            case DAB -> new Pose(
                    v(35, -20, 0),                            // face buried toward the elbow
                    v((float) osc(t, 16, 4), 0, 0),           // tiny bounce
                    v(-100, 0, 55),                           // straight back arm
                    v(-130, 0, -35),                          // elbow across the face
                    v(0, 0, -8), v(0, 0, 8), 0f);
            case SPIN -> new Pose(
                    v(0, 0, 0),
                    v(0, 0, 0),
                    v(0, 0, -90), v(0, 0, 90),                // arms straight out
                    v(0, 0, 0), v(0, 0, 0),
                    (t * 12f) % 360f);                        // 360° every 30 ticks
            case FLOSS -> {
                float swing = (float) osc(t, 16, 1);          // -1..1 metronome
                yield new Pose(
                        v(0, 6 * swing, 0),
                        v(0, 0, -10 * swing),                 // hips counter-swing
                        v(-90, 0, 35 * swing + 8),            // straight arms sweep together
                        v(-90, 0, 35 * swing - 8),
                        v(0, 0, 5 * swing), v(0, 0, -5 * swing), 0f);
            }
            case ROBOT -> {
                // Snap between stiff right-angle poses every 10 ticks.
                int phase = (t / 10) % 4;
                Vector3f left  = switch (phase) {
                    case 0 -> v(-90, 0, 0);
                    case 1 -> v(-90, -45, 0);
                    case 2 -> v(0, 0, -90);
                    default -> v(-45, 0, 0);
                };
                Vector3f right = switch (phase) {
                    case 0 -> v(0, 0, 90);
                    case 1 -> v(-90, 45, 0);
                    case 2 -> v(-90, 0, 0);
                    default -> v(-135, 0, 0);
                };
                yield new Pose(v(0, phase * 90f % 180 - 45, 0), v(0, 0, 0),
                        left, right, v(0, 0, 0), v(0, 0, 0), 0f);
            }
        };
    }

    /** Sine oscillation: amplitude {@code amp}, period {@code period} ticks. */
    private static double osc(int t, int period, double amp) {
        return amp * Math.sin((2 * Math.PI * t) / period);
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }
}

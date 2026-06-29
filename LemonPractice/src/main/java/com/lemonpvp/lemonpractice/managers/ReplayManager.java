package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelGame;
import net.kyori.adventure.text.minimessage.MiniMessage;
import com.lemonpvp.lemonpractice.replay.ArmorStandReplayActor;
import com.lemonpvp.lemonpractice.replay.NpcReplayActor;
import com.lemonpvp.lemonpractice.replay.ReplayActor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Efficient duel replay system.
 *
 * <p>While a duel is fighting, both players' position + rotation are sampled at a
 * fixed interval into compact 30-byte frames (floats for position, byte-angles
 * for yaw/pitch, a flags byte). On end the frames are GZIP-compressed; if the
 * result would exceed {@value #MAX_BYTES} bytes the frame rate is halved until it
 * fits, guaranteeing each stored replay is ≤ 50 KB. Replays are kept for 100 days
 * (extendable via {@code /replay <name> add <days>}) and named
 * {@code <player1>-<player2>-<yyyyMMdd>-<HHmmss>}.</p>
 *
 * <p>Playback spawns two named, player-headed armor stands that follow the
 * recorded path for the viewer.</p>
 */
public class ReplayManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int MAGIC = 0x4C525031;          // "LRP1"
    private static final byte VERSION = 1;
    private static final int MAX_BYTES = 50 * 1024;       // hard 50 KB cap
    public static final long RETENTION_DAYS = 100L;
    private static final long RETENTION_MS = RETENTION_DAYS * 86_400_000L;
    private static final int FRAME_BYTES = 30;            // 15 per player
    private static final int MAX_FRAMES = 4000;           // memory bound while recording

    private final LemonPractice plugin;
    private final int interval;                            // sample period in ticks
    private final Map<UUID, Recording> active = new ConcurrentHashMap<>();   // key = duel player1 uuid
    private final Map<UUID, Playback> playbacks = new ConcurrentHashMap<>(); // key = viewer uuid

    public ReplayManager(LemonPractice plugin) {
        this.plugin = plugin;
        this.interval = Math.max(1, plugin.getConfig().getInt("replays.sample-interval-ticks", 2));
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("replays.enabled", true);
    }

    // ── Recording ────────────────────────────────────────────────────────────────

    private static final class Recording {
        final UUID p1, p2;
        final String p1Name, p2Name, world, gamemode;
        final List<byte[]> frames = new ArrayList<>();
        BukkitTask task;
        Recording(DuelGame g, String world) {
            this.p1 = g.getPlayer1Uuid(); this.p2 = g.getPlayer2Uuid();
            this.p1Name = g.getPlayer1Name(); this.p2Name = g.getPlayer2Name();
            this.gamemode = g.getGamemode(); this.world = world;
        }
    }

    public void start(DuelGame game) {
        if (!enabled()) return;
        Player p1 = Bukkit.getPlayer(game.getPlayer1Uuid());
        if (p1 == null || p1.getWorld() == null) return;
        Recording r = new Recording(game, p1.getWorld().getName());
        r.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> captureFrame(r), interval, interval);
        active.put(game.getPlayer1Uuid(), r);
    }

    private void captureFrame(Recording r) {
        if (r.frames.size() >= MAX_FRAMES) { if (r.task != null) r.task.cancel(); return; }
        ByteBuffer buf = ByteBuffer.allocate(FRAME_BYTES);
        writePlayer(buf, Bukkit.getPlayer(r.p1));
        writePlayer(buf, Bukkit.getPlayer(r.p2));
        r.frames.add(buf.array());
    }

    private void writePlayer(ByteBuffer buf, Player p) {
        if (p == null || p.isDead() || p.getLocation() == null) {
            buf.putFloat(0).putFloat(0).putFloat(0).put((byte) 0).put((byte) 0).put((byte) 0x02); // GONE
            return;
        }
        Location l = p.getLocation();
        byte flags = (byte) (p.isSneaking() ? 0x01 : 0x00);
        buf.putFloat((float) l.getX()).putFloat((float) l.getY()).putFloat((float) l.getZ())
                .put(angle(l.getYaw())).put(angle(l.getPitch())).put(flags);
    }

    /** Stop recording a finished duel and persist it (async). */
    public void stop(DuelGame game) {
        Recording r = active.remove(game.getPlayer1Uuid());
        if (r == null) return;
        if (r.task != null) r.task.cancel();
        if (r.frames.size() < 5) return; // too short to be worth keeping
        persist(r, new ArrayList<>(r.frames));
    }

    /**
     * Stop + persist like {@link #stop}, and additionally show the loser an
     * automatic kill-cam of the last few seconds (in-memory, no DB round-trip).
     */
    public void stopWithKillCam(DuelGame game, UUID loserUuid) {
        Recording r = active.remove(game.getPlayer1Uuid());
        if (r == null) return;
        if (r.task != null) r.task.cancel();
        if (r.frames.size() < 5) return;

        final List<byte[]> frames = new ArrayList<>(r.frames);
        persist(r, frames);

        if (!plugin.getConfig().getBoolean("replays.killcam.enabled", true)) return;
        if (loserUuid == null || frames.size() < 10) return;

        int secs = Math.max(1, plugin.getConfig().getInt("replays.killcam.seconds", 4));
        double sp = plugin.getConfig().getDouble("replays.killcam.speed", 0.6);
        int tail = Math.min(frames.size(), secs * 20 / Math.max(1, interval));
        Decoded d = new Decoded();
        d.p1 = r.p1Name; d.p2 = r.p2Name; d.world = r.world; d.interval = interval;
        d.frames = frames.subList(frames.size() - tail, frames.size()).toArray(new byte[0][]);
        int followIdx = loserUuid.equals(r.p2) ? 2 : 1; // follow the loser

        // Let the duel's own cleanup (lobby teleport) finish first, then roll the clip.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player loser = Bukkit.getPlayer(loserUuid);
            if (loser == null || !loser.isOnline()) return;
            loser.sendMessage(MM.deserialize("<gradient:#ff5252:#b71c1c><bold>Kill-Cam</bold></gradient> "
                    + "<gray>Replaying the final moments… <white>/replay stop <gray>to skip."));
            playDecoded(loser, d, "killcam", followIdx, sp);
        }, 30L);
    }

    private void persist(Recording r, List<byte[]> frames) {
        final String name = buildName(r.p1Name, r.p2Name);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            byte[] data = encodeWithinCap(frames, r);
            if (data == null) {
                plugin.getLogger().warning("[Replay] " + name + " exceeded 50KB even downsampled — skipped.");
                return;
            }
            long now = System.currentTimeMillis();
            plugin.getDatabase().saveReplay(name, r.p1Name, r.p2Name, r.gamemode, now, now + RETENTION_MS, data);
            plugin.getLogger().info("[Replay] Saved " + name + " (" + data.length + " bytes, "
                    + frames.size() + " frames).");
        });
    }

    /** Encodes frames, halving the frame rate until the GZIP payload fits in 50 KB. */
    private byte[] encodeWithinCap(List<byte[]> frames, Recording r) {
        int effInterval = interval;
        List<byte[]> f = frames;
        for (int attempt = 0; attempt < 8; attempt++) {
            byte[] data = buildPayload(f, r, effInterval);
            if (data != null && data.length <= MAX_BYTES) return data;
            if (f.size() <= 10) return null;
            f = downsample(f);
            effInterval *= 2;
        }
        return null;
    }

    private byte[] buildPayload(List<byte[]> frames, Recording r, int effInterval) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bos))) {
            out.writeInt(MAGIC);
            out.writeByte(VERSION);
            out.writeUTF(r.p1Name);
            out.writeUTF(r.p2Name);
            out.writeUTF(r.world);
            out.writeInt(effInterval);
            out.writeInt(frames.size());
            for (byte[] frame : frames) out.write(frame);
        } catch (Exception e) {
            plugin.getLogger().warning("[Replay] encode error: " + e.getMessage());
            return null;
        }
        return bos.toByteArray();
    }

    private List<byte[]> downsample(List<byte[]> frames) {
        List<byte[]> out = new ArrayList<>(frames.size() / 2 + 1);
        for (int i = 0; i < frames.size(); i += 2) out.add(frames.get(i));
        return out;
    }

    // ── Playback ─────────────────────────────────────────────────────────────────

    private static final class Decoded {
        String p1, p2, world; int interval; byte[][] frames;
    }

    private static final class Playback {
        final UUID viewer;
        final World world;
        final ReplayActor a1, a2;
        final Decoded d;
        final String name;
        double index;          // fractional frame position
        int lastRendered = -1; // last frame index actually drawn
        double speed = 1.0;
        boolean paused = false;
        int follow = 0;        // 0 = free cam, 1 = actor1, 2 = actor2
        BukkitTask task;
        org.bukkit.Location originLoc;     // where the viewer was before watching
        org.bukkit.GameMode originMode;
        Playback(UUID viewer, World world, ReplayActor a1, ReplayActor a2, Decoded d, String name) {
            this.viewer = viewer; this.world = world; this.a1 = a1; this.a2 = a2; this.d = d; this.name = name;
        }
    }

    public void play(Player viewer, String name) {
        UUID vu = viewer.getUniqueId();
        stopPlayback(vu); // one at a time
        plugin.getDatabase().loadReplayData(name).thenAccept(bytes -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player v = Bukkit.getPlayer(vu);
            if (v == null) return;
            if (bytes == null) {
                v.sendMessage(MM.deserialize("<red>Replay <yellow>" + name + " <red>not found or expired."));
                return;
            }
            Decoded d = decode(bytes);
            if (d == null || d.frames.length == 0) {
                v.sendMessage(MM.deserialize("<red>That replay is corrupted."));
                return;
            }
            World world = Bukkit.getWorld(d.world);
            if (world == null) {
                v.sendMessage(MM.deserialize("<red>Replay world '<yellow>" + d.world + "<red>' is not loaded."));
                return;
            }
            startPlayback(v, world, d, name, 0, 1.0, true);
        }));
    }

    /** Plays an already-decoded clip (used by the kill-cam, which has no DB row). */
    private void playDecoded(Player viewer, Decoded d, String name, int follow, double speed) {
        if (d == null || d.frames.length == 0) return;
        World world = Bukkit.getWorld(d.world);
        if (world == null) return;
        startPlayback(viewer, world, d, name, follow, speed, false);
    }

    /** Shared playback setup: teleport the viewer in (spectator), spawn actors, start ticking. */
    private void startPlayback(Player v, World world, Decoded d, String name,
                               int follow, double speed, boolean announce) {
        UUID vu = v.getUniqueId();
        stopPlayback(vu); // one at a time

        ReplayActor a1 = createActor(v, world, d.frames[0], 0, d.p1);
        ReplayActor a2 = createActor(v, world, d.frames[0], 15, d.p2);
        Playback pb = new Playback(vu, world, a1, a2, d, name);
        pb.follow = follow;
        pb.speed = speed;

        // Remember where the viewer was, then move them into the replay as a spectator.
        pb.originLoc = v.getLocation().clone();
        pb.originMode = v.getGameMode();
        try { v.setGameMode(org.bukkit.GameMode.SPECTATOR); } catch (Throwable ignored) {}
        v.teleport(vantage(world, d.frames[0]));

        if (announce) {
            v.sendMessage(MM.deserialize("<green>Playing replay <yellow>" + name
                    + " <gray>(" + d.frames.length + " frames). "
                    + "<gray>Controls: <white>/replay pause<gray>, <white>speed <x><gray>, "
                    + "<white>follow <1|2|off><gray>, <white>restart<gray>, <white>stop"));
        }
        pb.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(pb), 0L, 1L);
        playbacks.put(vu, pb);
    }

    /** A vantage point overlooking the two actors at the given frame. */
    private Location vantage(World world, byte[] frame) {
        Location p1 = readLoc(world, frame, 0);
        Location p2 = readLoc(world, frame, 15);
        double mx = (p1.getX() + p2.getX()) / 2.0;
        double my = Math.max(p1.getY(), p2.getY());
        double mz = (p1.getZ() + p2.getZ()) / 2.0;
        Location cam = new Location(world, mx + 4, my + 4, mz + 4);
        org.bukkit.util.Vector look = new org.bukkit.util.Vector(mx, my + 1, mz).subtract(cam.toVector());
        cam.setDirection(look);
        return cam;
    }

    /** Runs every tick; advances the (fractional) frame position by speed/interval. */
    private void tick(Playback pb) {
        Player v = Bukkit.getPlayer(pb.viewer);
        if (v == null || !v.isOnline()) { finishPlayback(pb, false); return; }

        if (!pb.paused) pb.index += pb.speed / Math.max(1, pb.d.interval);
        int i = (int) Math.floor(pb.index);
        if (i >= pb.d.frames.length) { finishPlayback(pb, true); return; }

        if (i != pb.lastRendered) {
            pb.lastRendered = i;
            byte[] frame = pb.d.frames[i];
            if ((frame[14] & 0x02) == 0) pb.a1.teleport(readLoc(pb.world, frame, 0));        // skip GONE
            if ((frame[15 + 14] & 0x02) == 0) pb.a2.teleport(readLoc(pb.world, frame, 15));
            if (pb.follow != 0) applyFollow(v, pb, frame);
        }
        sendHud(v, pb, i);
    }

    /** Over-the-shoulder camera that follows one actor. */
    private void applyFollow(Player v, Playback pb, byte[] frame) {
        int off = (pb.follow == 2) ? 15 : 0;
        if ((frame[off + 14] & 0x02) != 0) return; // followed actor is gone this frame
        Location target = readLoc(pb.world, frame, off);
        org.bukkit.util.Vector dir = target.getDirection().setY(0).normalize();
        if (Double.isNaN(dir.getX())) dir = new org.bukkit.util.Vector(0, 0, 1);
        Location cam = target.clone().subtract(dir.multiply(3.2)).add(0, 1.8, 0);
        cam.setYaw(target.getYaw());
        cam.setPitch(12f);
        v.teleport(cam);
    }

    private void sendHud(Player v, Playback pb, int i) {
        String state = pb.paused ? "<red>⏸ Paused" : "<green>▶ " + trimSpeed(pb.speed) + "x";
        String foll = pb.follow == 0 ? "" : " <dark_gray>| <aqua>following " + (pb.follow == 1 ? pb.d.p1 : pb.d.p2);
        v.sendActionBar(MM.deserialize("<!italic>" + state + " <dark_gray>| <gray>" + i + "<dark_gray>/<gray>"
                + pb.d.frames.length + foll));
    }

    private String trimSpeed(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.valueOf(d);
    }

    // ── Playback controls ────────────────────────────────────────────────────────

    public boolean togglePause(UUID viewer) {
        Playback pb = playbacks.get(viewer);
        if (pb == null) return false;
        pb.paused = !pb.paused;
        return true;
    }

    public boolean setSpeed(UUID viewer, double speed) {
        Playback pb = playbacks.get(viewer);
        if (pb == null) return false;
        pb.speed = Math.max(0.1, Math.min(8.0, speed));
        return true;
    }

    public boolean restart(UUID viewer) {
        Playback pb = playbacks.get(viewer);
        if (pb == null) return false;
        pb.index = 0; pb.lastRendered = -1; pb.paused = false;
        return true;
    }

    /** follow: 0 free, 1 actor1, 2 actor2. */
    public boolean setFollow(UUID viewer, int follow) {
        Playback pb = playbacks.get(viewer);
        if (pb == null) return false;
        pb.follow = follow;
        return true;
    }

    public boolean isWatching(UUID viewer) {
        return playbacks.containsKey(viewer);
    }

    /**
     * Builds an actor for the viewer: a real player-model NPC via PacketEvents
     * when it is installed, otherwise the armor-stand fallback. Any failure
     * loading/using PacketEvents falls back gracefully.
     */
    private ReplayActor createActor(Player viewer, World world, byte[] frame, int off, String name) {
        Location loc = readLoc(world, frame, off);
        if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            try {
                NpcReplayActor npc = new NpcReplayActor(plugin, viewer, name);
                npc.spawn(loc);
                return npc;
            } catch (Throwable t) {
                plugin.getLogger().warning("[Replay] PacketEvents NPC failed, using armor stand: " + t);
            }
        }
        return new ArmorStandReplayActor(world, loc, name);
    }

    private Location readLoc(World world, byte[] frame, int off) {
        ByteBuffer buf = ByteBuffer.wrap(frame, off, 15);
        float x = buf.getFloat(), y = buf.getFloat(), z = buf.getFloat();
        float yaw = unangle(buf.get()), pitch = unangle(buf.get());
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void stopPlayback(UUID viewer) {
        Playback pb = playbacks.remove(viewer);
        if (pb != null) finishPlayback(pb, false);
    }

    private void finishPlayback(Playback pb, boolean announce) {
        if (pb.task != null) pb.task.cancel();
        if (pb.a1 != null) pb.a1.remove();
        if (pb.a2 != null) pb.a2.remove();
        playbacks.remove(pb.viewer);

        // Restore the viewer to where they were watching from.
        Player v = Bukkit.getPlayer(pb.viewer);
        if (v != null) {
            try { if (pb.originMode != null) v.setGameMode(pb.originMode); } catch (Throwable ignored) {}
            if (pb.originLoc != null) v.teleport(pb.originLoc);
            if (announce) v.sendMessage(MM.deserialize("<gray>Replay finished."));
        }
    }

    private Decoded decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))) {
            if (in.readInt() != MAGIC) return null;
            in.readByte(); // version
            Decoded d = new Decoded();
            d.p1 = in.readUTF(); d.p2 = in.readUTF(); d.world = in.readUTF();
            d.interval = Math.max(1, in.readInt());
            int count = in.readInt();
            if (count < 0 || count > MAX_FRAMES * 4) return null;
            d.frames = new byte[count][FRAME_BYTES];
            for (int i = 0; i < count; i++) in.readFully(d.frames[i]);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Lifecycle / helpers ──────────────────────────────────────────────────────

    public void shutdown() {
        for (Recording r : active.values()) if (r.task != null) r.task.cancel();
        active.clear();
        for (UUID v : new ArrayList<>(playbacks.keySet())) stopPlayback(v);
    }

    /** Periodic retention cleanup of expired replays. */
    public void purgeExpired() {
        plugin.getDatabase().deleteExpiredReplays().thenAccept(n -> {
            if (n != null && n > 0) plugin.getLogger().info("[Replay] Purged " + n + " expired replays.");
        });
    }

    private String buildName(String p1, String p2) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return sanitize(p1) + "-" + sanitize(p2) + "-" + ts;
    }

    private String sanitize(String s) {
        if (s == null || s.isEmpty()) return "unknown";
        return s.replaceAll("[^A-Za-z0-9_]", "");
    }

    private static byte angle(float a) {
        return (byte) Math.round(a * 256.0f / 360.0f);
    }

    private static float unangle(byte b) {
        return b * 360.0f / 256.0f;
    }
}

package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ExplosionPreset;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explosion-particle cosmetics: when a player's end crystal or primed TNT
 * explodes, their equipped preset's particle burst + sound plays at the
 * explosion. Presets are the 25 built-ins below plus any number of
 * owner-defined presets from {@code explosions.lemon} (id, name, price,
 * particle, sound, icon) — reloaded without recompiling.
 *
 * <p>Crystal attribution: the last player to damage an end crystal (within a
 * short window) owns its explosion; {@code ExplosionListener} feeds that in.
 * Everything visual runs on the main thread; DB work is async.</p>
 */
public class ExplosionParticleManager {

    /** How long a crystal hit stays attributable (ms). */
    private static final long ATTRIBUTION_TTL_MS = 10_000L;

    private final LemonCosmetics plugin;

    /** Built-in + .lemon presets; swapped atomically on reload. */
    private volatile List<ExplosionPreset> presets = List.of();

    /** crystal entityId -> (owner uuid, timestamp) for explosion attribution. */
    private final Map<Integer, long[]> crystalHitTimes = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> crystalHitters = new ConcurrentHashMap<>();

    public ExplosionParticleManager(LemonCosmetics plugin) {
        this.plugin = plugin;
        reload();
    }

    // -----------------------------------------------------------------------
    // Presets: built-ins + explosions.lemon
    // -----------------------------------------------------------------------

    private static List<ExplosionPreset> builtins() {
        List<ExplosionPreset> b = new ArrayList<>();
        b.add(p("soul_blast",     "Soul Blast",      600, Material.SOUL_LANTERN,     Particle.SOUL_FIRE_FLAME,  Sound.PARTICLE_SOUL_ESCAPE));
        b.add(p("cherry_bomb",    "Cherry Bomb",     600, Material.CHERRY_SAPLING,   Particle.CHERRY_LEAVES,    Sound.BLOCK_AMETHYST_BLOCK_CHIME));
        b.add(p("glow_nova",      "Glow Nova",       700, Material.GLOW_INK_SAC,     Particle.GLOW,             Sound.BLOCK_AMETHYST_BLOCK_CHIME));
        b.add(p("sculk_burst",    "Sculk Burst",     800, Material.SCULK,            Particle.SCULK_SOUL,       Sound.BLOCK_SCULK_CATALYST_BLOOM));
        b.add(p("ender_rift",     "Ender Rift",      700, Material.ENDER_EYE,        Particle.PORTAL,           Sound.ENTITY_ENDERMAN_TELEPORT));
        b.add(p("frost_bomb",     "Frost Bomb",      600, Material.BLUE_ICE,         Particle.SNOWFLAKE,        Sound.BLOCK_GLASS_BREAK));
        b.add(p("firework_nuke",  "Firework Nuke",   800, Material.FIREWORK_ROCKET,  Particle.FIREWORK,         Sound.ENTITY_FIREWORK_ROCKET_BLAST));
        b.add(p("lava_geyser",    "Lava Geyser",     700, Material.MAGMA_BLOCK,      Particle.LAVA,             Sound.BLOCK_LAVA_POP));
        b.add(p("emerald_boom",   "Emerald Boom",    600, Material.EMERALD,          Particle.HAPPY_VILLAGER,   Sound.ENTITY_VILLAGER_CELEBRATE));
        b.add(p("void_implosion", "Void Implosion",  900, Material.OBSIDIAN,         Particle.REVERSE_PORTAL,   Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE));
        b.add(p("heartburst",     "Heartburst",      500, Material.PINK_DYE,         Particle.HEART,            Sound.ENTITY_PLAYER_LEVELUP));
        b.add(p("thunderclap",    "Thunderclap",     800, Material.LIGHTNING_ROD,    Particle.ELECTRIC_SPARK,   Sound.ENTITY_LIGHTNING_BOLT_THUNDER));
        b.add(p("witchs_brew",    "Witch's Brew",    700, Material.BREWING_STAND,    Particle.WITCH,            Sound.ENTITY_WITCH_CELEBRATE));
        b.add(p("ink_bomb",       "Ink Bomb",        500, Material.INK_SAC,          Particle.SQUID_INK,        Sound.ENTITY_SQUID_SQUIRT));
        b.add(p("glowing_ink",    "Glowing Ink",     600, Material.GLOW_BERRIES,     Particle.GLOW_SQUID_INK,   Sound.ENTITY_GLOW_SQUID_AMBIENT));
        b.add(p("cloud_pop",      "Cloud Pop",       400, Material.WHITE_WOOL,       Particle.CLOUD,            Sound.ENTITY_BREEZE_SHOOT));
        b.add(p("crit_shock",     "Crit Shock",      500, Material.IRON_SWORD,       Particle.CRIT,             Sound.ENTITY_PLAYER_ATTACK_CRIT));
        b.add(p("enchant_storm",  "Enchant Storm",   600, Material.ENCHANTING_TABLE, Particle.ENCHANT,          Sound.BLOCK_ENCHANTMENT_TABLE_USE));
        b.add(p("dragon_blast",   "Dragon Blast",   1000, Material.DRAGON_BREATH,    Particle.DRAGON_BREATH,    Sound.ENTITY_ENDER_DRAGON_GROWL));
        b.add(p("nature_pop",     "Nature Pop",      500, Material.OAK_SAPLING,      Particle.COMPOSTER,        Sound.BLOCK_GRASS_BREAK));
        b.add(p("angelic_flash",  "Angelic Flash",   800, Material.END_ROD,          Particle.END_ROD,          Sound.BLOCK_BELL_USE));
        b.add(p("smokescreen",    "Smokescreen",     500, Material.CAMPFIRE,         Particle.CAMPFIRE_COSY_SMOKE, Sound.BLOCK_FIRE_EXTINGUISH));
        b.add(p("wax_spark",      "Wax Spark",       500, Material.HONEYCOMB,        Particle.WAX_ON,           Sound.ENTITY_EXPERIENCE_ORB_PICKUP));
        b.add(p("ash_cloud",      "Ash Cloud",       400, Material.TUFF,             Particle.ASH,              Sound.BLOCK_DEEPSLATE_BREAK));
        b.add(p("totem_nova",     "Totem Nova",      900, Material.TOTEM_OF_UNDYING, Particle.TOTEM_OF_UNDYING, Sound.ITEM_TOTEM_USE));
        return b;
    }

    private static ExplosionPreset p(String id, String name, int price,
                                     Material icon, Particle particle, Sound sound) {
        return new ExplosionPreset(id, name, price, icon, particle, sound, true);
    }

    /**
     * Reloads presets: built-ins + explosions.lemon. Custom presets may not
     * shadow built-in ids. Invalid entries are skipped with a warning.
     */
    public void reload() {
        List<ExplosionPreset> merged = new ArrayList<>(builtins());
        var knownIds = new java.util.HashSet<String>();
        for (ExplosionPreset preset : merged) knownIds.add(preset.id());

        File file = new File(plugin.getDataFolder(), "explosions.lemon");
        if (!file.exists()) {
            try { plugin.saveResource("explosions.lemon", false); } catch (Exception ignored) {}
        }

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                String id = null, name = null, particle = null, sound = null, icon = null;
                int price = 500;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (line.startsWith("explosion ")) {
                        addCustom(merged, knownIds, id, name, price, particle, sound, icon);
                        id = line.substring(10).trim().toLowerCase(Locale.ROOT);
                        name = id; price = 500; particle = null; sound = null; icon = null;
                    } else if (id != null) {
                        String[] parts = line.split("\\s+", 2);
                        if (parts.length < 2) continue;
                        String value = parts[1].replace("\"", "").trim();
                        switch (parts[0]) {
                            case "name"     -> name = value;
                            case "price"    -> price = parseInt(value, 500);
                            case "particle" -> particle = value;
                            case "sound"    -> sound = value;
                            case "icon"     -> icon = value;
                        }
                    }
                }
                addCustom(merged, knownIds, id, name, price, particle, sound, icon);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load explosions.lemon: " + e.getMessage());
            }
        }

        this.presets = List.copyOf(merged);
        plugin.getLogger().info("Loaded " + presets.size() + " explosion presets ("
                + (presets.size() - builtins().size()) + " from explosions.lemon).");
    }

    private void addCustom(List<ExplosionPreset> merged, java.util.Set<String> knownIds,
                           String id, String name, int price,
                           String particle, String sound, String icon) {
        if (id == null) return;
        if (knownIds.contains(id)) {
            plugin.getLogger().warning("[explosions.lemon] duplicate/built-in id '" + id + "' skipped.");
            return;
        }
        Particle par;
        try {
            par = Particle.valueOf(particle == null ? "" : particle.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[explosions.lemon] '" + id + "': unknown particle '" + particle + "' — skipped.");
            return;
        }
        Sound snd = null;
        if (sound != null) {
            try { snd = Sound.valueOf(sound.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[explosions.lemon] '" + id + "': unknown sound '" + sound + "' — no sound.");
            }
        }
        Material mat = Material.matchMaterial(icon == null ? "TNT" : icon);
        if (mat == null) mat = Material.TNT;
        merged.add(new ExplosionPreset(id, name != null ? name : id, Math.max(0, price), mat, par, snd, false));
        knownIds.add(id);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.replace("_", "").trim()); }
        catch (NumberFormatException e) { return def; }
    }

    // -----------------------------------------------------------------------
    // Queries / access / unlock / buy
    // -----------------------------------------------------------------------

    public List<ExplosionPreset> getAll() {
        return presets;
    }

    public ExplosionPreset fromId(String id) {
        if (id == null) return null;
        for (ExplosionPreset preset : presets) {
            if (preset.id().equalsIgnoreCase(id)) return preset;
        }
        return null;
    }

    /** Whether the player may equip this preset: owned or granted by permission. */
    public boolean canUse(Player player, ExplosionPreset preset) {
        if (player.hasPermission(preset.permission())) return true;
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        return cosmetics != null && cosmetics.ownsExplosion(preset.id());
    }

    /** Adds the preset to the player's owned set (cache + DB) without equipping it. */
    public CompletableFuture<Void> unlock(UUID uuid, String presetId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.getOwnedExplosions().add(presetId);
        return plugin.getDatabase().saveExplosion(uuid, presetId, false);
    }

    /** Buys a preset with coins. Returns {@code false} if unaffordable. */
    public CompletableFuture<Boolean> buy(UUID uuid, String presetId) {
        ExplosionPreset preset = fromId(presetId);
        if (preset == null) return CompletableFuture.completedFuture(false);
        return plugin.getCosmeticsManager().canAfford(uuid, preset.price()).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);
            var lc = plugin.getCosmeticsManager().getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);
            return lc.getPlayerDataManager()
                    .removeCoins(uuid, preset.price(), "cosmetics:explosion:" + presetId, null)
                    .thenCompose(v -> unlock(uuid, presetId))
                    .thenApply(v -> true);
        });
    }

    /** Sets (or clears, when {@code presetId} is null) the active preset. */
    public CompletableFuture<Void> setActive(UUID uuid, String presetId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setActiveExplosionId(presetId);
        return presetId == null
                ? plugin.getDatabase().clearActiveExplosion(uuid)
                : plugin.getDatabase().setActiveExplosion(uuid, presetId);
    }

    // -----------------------------------------------------------------------
    // Attribution + rendering
    // -----------------------------------------------------------------------

    /** Remembers who last hit an end crystal so its explosion can be attributed. */
    public void recordCrystalHit(int crystalEntityId, UUID playerUuid) {
        long now = System.currentTimeMillis();
        crystalHitters.put(crystalEntityId, playerUuid);
        crystalHitTimes.put(crystalEntityId, new long[]{now});
        // Opportunistic TTL cleanup so the maps stay bounded.
        if (crystalHitters.size() > 256) {
            crystalHitTimes.forEach((id, ts) -> {
                if (now - ts[0] > ATTRIBUTION_TTL_MS) {
                    crystalHitTimes.remove(id);
                    crystalHitters.remove(id);
                }
            });
        }
    }

    /** Returns and consumes the attributed owner of a crystal explosion, or null. */
    public UUID consumeCrystalOwner(int crystalEntityId) {
        long[] ts = crystalHitTimes.remove(crystalEntityId);
        UUID owner = crystalHitters.remove(crystalEntityId);
        if (owner == null || ts == null) return null;
        return (System.currentTimeMillis() - ts[0] <= ATTRIBUTION_TTL_MS) ? owner : null;
    }

    /** Plays {@code owner}'s equipped preset at the explosion location, if any. */
    public void playFor(UUID owner, Location loc) {
        if (owner == null) return;
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(owner);
        if (cosmetics == null) return;
        ExplosionPreset preset = fromId(cosmetics.getActiveExplosionId());
        if (preset == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> playBurst(preset, loc));
    }

    /**
     * Explosion burst: an expanding sphere shell over ~10 ticks. Heavy one-shot
     * particles (EXPLOSION, SONIC_BOOM) are rendered compactly instead.
     */
    private int pc(int n) { return plugin.particleCount(n); }

    private void playBurst(ExplosionPreset preset, Location loc) {
        final World world = loc.getWorld();
        if (world == null) return;
        if (pc(1) <= 0) return;
        if (preset.sound() != null) world.playSound(loc, preset.sound(), 1.0f, 1.0f);

        if (preset.particle() == Particle.EXPLOSION || preset.particle() == Particle.SONIC_BOOM) {
            world.spawnParticle(preset.particle(), loc.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            return;
        }

        final Location base = loc.clone().add(0, 0.5, 0);
        // Big opening flash for immediate punch.
        world.spawnParticle(preset.particle(), base, pc(130), 0.9, 0.9, 0.9, 0.09);

        final int[] t = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (base.getWorld() == null || t[0] >= 14) { task[0].cancel(); return; }
            // Two concentric expanding shells for a fuller blast.
            double r = 0.6 + t[0] * 0.52;
            double r2 = r * 0.6;
            int pts = 56 + t[0] * 6;
            for (int i = 0; i < pts; i++) {
                // Fibonacci sphere shell points for an even, dense burst.
                double theta = i * 2.399963;
                double y = 1.0 - (i / (double) (pts - 1)) * 2.0;
                double rr = Math.sqrt(Math.max(0, 1 - y * y));
                base.getWorld().spawnParticle(preset.particle(),
                        base.clone().add(Math.cos(theta) * rr * r, y * r * 0.75, Math.sin(theta) * rr * r),
                        pc(1), 0.02, 0.02, 0.02, 0);
                if (i % 2 == 0) {
                    base.getWorld().spawnParticle(preset.particle(),
                            base.clone().add(Math.cos(theta) * rr * r2, y * r2 * 0.75, Math.sin(theta) * rr * r2),
                            pc(1), 0.02, 0.02, 0.02, 0);
                }
            }
            t[0]++;
        }, 0L, 1L);
    }
}

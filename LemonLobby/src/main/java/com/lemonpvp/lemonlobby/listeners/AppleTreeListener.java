package com.lemonpvp.lemonlobby.listeners;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.model.PlankTier;
import com.lemonpvp.lemonlobby.util.EconomyBridge;
import com.lemonpvp.lemonlobby.util.HologramUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.time.Duration;
import java.util.*;

public class AppleTreeListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final long   COMBO_WINDOW_MS  = 2_000;
    private static final int    MAX_COMBO        = 5;
    private static final double JACKPOT_CHANCE   = 0.03;  // 3 % per leaf-click
    private static final int    JACKPOT_BONUS    = 25;

    private final LemonLobby plugin;
    private final Map<UUID, Long>    appleCooldowns = new HashMap<>();
    private final Map<UUID, Long>    plankCooldowns = new HashMap<>();
    private final Map<UUID, Integer> comboCount     = new HashMap<>();
    private final Map<UUID, Long>    lastClickTime  = new HashMap<>();

    // Cached from config on reload()
    private Set<Material> leafMaterials;
    private Set<Material> logMaterials;
    private long   appleCooldownMs;
    private long   plankCooldownMs;
    private String worldName;
    private int    baseApples;

    public AppleTreeListener(LemonLobby plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        leafMaterials = new HashSet<>();
        logMaterials  = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("apple-tree.leaf-materials")) {
            try { leafMaterials.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
        for (String s : plugin.getConfig().getStringList("apple-tree.log-materials")) {
            try {
                Material m = Material.valueOf(s);
                logMaterials.add(m);
                // Auto-include the debarked/wood variants of every configured log
                // so stripped trunks (no bark) stay harvestable too.
                String base = m.name().replace("STRIPPED_", "")
                        .replace("_WOOD", "").replace("_LOG", "");
                for (String variant : new String[]{
                        "STRIPPED_" + base + "_LOG", "STRIPPED_" + base + "_WOOD", base + "_WOOD"}) {
                    try { logMaterials.add(Material.valueOf(variant)); }
                    catch (IllegalArgumentException ignored) {}
                }
            } catch (IllegalArgumentException ignored) {}
        }
        appleCooldownMs = plugin.getConfig().getLong("apple-tree.apple-cooldown-ticks", 20) * 50L;
        plankCooldownMs = plugin.getConfig().getLong("apple-tree.plank-cooldown-ticks", 100) * 50L;
        worldName  = plugin.getConfig().getString("apple-tree.world", "world");
        baseApples = plugin.getConfig().getInt("apple-tree.base-apples", 1);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (!e.getPlayer().getWorld().getName().equals(worldName)) return;

        Material type = block.getType();
        boolean treeBlock = leafMaterials.contains(type) || logMaterials.contains(type);
        if (!treeBlock) return;

        // Äpfel/Planks gibt es NUR per Interact (Rechtsklick). Schlagen
        // (Linksklick/Hit) auf einen Baum-Block darf niemals ernten oder den
        // Block beschädigen — daher hier hart abbrechen.
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        if (leafMaterials.contains(type)) {
            handleLeafClick(player, block);
        } else if (logMaterials.contains(type)) {
            handleLogClick(player, block);
        }
    }

    /**
     * Verhindert das Abbauen von Baum-Blöcken (Blätter/Stämme) komplett.
     * Damit kann man durch <em>Schlagen</em> weder den Baum zerstören noch die
     * Vanilla-Apfel-Drops von Eichenlaub abgreifen — Ernte läuft ausschließlich
     * über Rechtsklick (Interact).
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.getPlayer().getWorld().getName().equals(worldName)) return;
        Material type = e.getBlock().getType();
        if (leafMaterials.contains(type) || logMaterials.contains(type)) {
            e.setCancelled(true);
        }
    }

    // ── Leaf → Apples ─────────────────────────────────────────────────────────

    private void handleLeafClick(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        Long last = appleCooldowns.get(uuid);
        if (last != null && now - last < appleCooldownMs) {
            long rem = appleCooldownMs - (now - last);
            player.sendActionBar(MM.deserialize(
                    "<!italic><red>⏳ <white>" + String.format("%.1f", rem / 1000.0) + "s"));
            return;
        }
        appleCooldowns.put(uuid, now);

        int combo  = advanceCombo(uuid, now);
        int bonus  = plugin.getBoosterManager().getBonus(uuid);
        double mult = 1.0 + (double)(combo - 1) / MAX_COMBO;
        boolean jackpot = Math.random() < JACKPOT_CHANCE;
        int total = (int) Math.round((baseApples + bonus) * mult) + (jackpot ? JACKPOT_BONUS : 0);

        // Golden Hour: server-wide apple multiplier.
        double goldenHour = plugin.getGoldenHourManager().getMultiplier();
        if (goldenHour > 1.0) total = (int) Math.round(total * goldenHour);

        EconomyBridge.addApples(uuid, total);

        org.bukkit.Location center = block.getLocation().add(0.5, 0.5, 0.5);

        if (jackpot) {
            handleJackpot(player, block, center, total);
        } else {
            // Sound beim Ernten entfernt (nur noch Partikel/Hologramm).

            // Particles
            int pCount = 4 + combo * 2;
            block.getWorld().spawnParticle(
                    bonus > 0 ? Particle.TOTEM_OF_UNDYING : Particle.HAPPY_VILLAGER,
                    center, pCount, 0.3, 0.3, 0.3, bonus > 0 ? 0.05 : 0);

            // Action-bar
            String comboTag  = combo > 1 ? " <gold>×" + combo : "";
            String boostTag  = bonus > 0 ? " <gray>(<yellow>+<white>" + bonus + "<gray>)" : "";
            String gradient  = combo >= MAX_COMBO ? "<gradient:#fffb00:#ff6600>" : "<green>";
            player.sendActionBar(MM.deserialize(
                    "<!italic>" + gradient + "+<white>" + total + " <green>✿" + comboTag + boostTag));

            // Floating hologram
            HologramUtil.spawnRising(plugin, center.clone().add(0, 1, 0),
                    MM.deserialize("<!italic><bold><green>+" + total + " ✿"
                            + (combo > 1 ? " <gold>×" + combo : "")));
        }
    }

    private void handleJackpot(Player player, Block block, org.bukkit.Location center, int total) {
        // Firework-ring particles
        for (int i = 0; i < 16; i++) {
            double angle = (2 * Math.PI / 16) * i;
            double dx = Math.cos(angle) * 0.8;
            double dz = Math.sin(angle) * 0.8;
            block.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    center.clone().add(dx, 0.5, dz), 4, 0, 0.4, 0, 0.08);
        }
        block.getWorld().spawnParticle(Particle.FIREWORK, center, 20, 0.4, 0.4, 0.4, 0.1);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);

        player.showTitle(Title.title(
                MM.deserialize("<!italic><bold><gradient:#fffb00:#ff6600>★ JACKPOT! ★"),
                MM.deserialize("<!italic><white>+" + total + " <green>✿ Bonus!"),
                Title.Times.times(
                        Duration.ofMillis(150),
                        Duration.ofMillis(1500),
                        Duration.ofMillis(400))));

        HologramUtil.spawnRising(plugin, center.clone().add(0, 1, 0),
                MM.deserialize("<!italic><bold><gradient:#fffb00:#ff6600>★ +" + total + " ✿ ★"));
    }

    // ── Log → Planks ──────────────────────────────────────────────────────────

    private void handleLogClick(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        Long last = plankCooldowns.get(uuid);
        if (last != null && now - last < plankCooldownMs) {
            long rem = plankCooldownMs - (now - last);
            player.sendActionBar(MM.deserialize(
                    "<!italic><red>⏳ <white>" + String.format("%.1f", rem / 1000.0) + "s"));
            return;
        }
        plankCooldowns.put(uuid, now);

        PlankTier tier  = plugin.getTreeUpgradeManager().getCurrentTier(uuid);
        int planks       = tier.planksPerClick;

        EconomyBridge.addPlanks(uuid, planks);

        // Sound beim Ernten entfernt (nur noch Partikel/Hologramm).
        org.bukkit.Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.BLOCK, center, 12, 0.3, 0.3, 0.3, block.getBlockData());

        player.sendActionBar(MM.deserialize(
                "<!italic><#D2691E>+<white>" + planks + " <#D2691E>▬ <gray>(" + tier.displayName + ")"));

        HologramUtil.spawnRising(plugin, center.clone().add(0, 1, 0),
                MM.deserialize("<!italic><bold><#D2691E>+" + planks + " ▬"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int advanceCombo(UUID uuid, long now) {
        Long lastTime = lastClickTime.get(uuid);
        int current   = comboCount.getOrDefault(uuid, 0);
        int next = (lastTime != null && now - lastTime <= COMBO_WINDOW_MS)
                ? Math.min(current + 1, MAX_COMBO)
                : 1;
        comboCount.put(uuid, next);
        lastClickTime.put(uuid, now);
        return next;
    }

    public boolean isTreeBlock(Block block) {
        if (block == null) return false;
        return leafMaterials.contains(block.getType()) || logMaterials.contains(block.getType());
    }

    public void cleanupPlayer(UUID uuid) {
        appleCooldowns.remove(uuid);
        plankCooldowns.remove(uuid);
        comboCount.remove(uuid);
        lastClickTime.remove(uuid);
    }
}

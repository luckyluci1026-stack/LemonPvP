package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Arena;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bot fallback duels: when nobody suitable is in the queue, the player fights a
 * kit-equipped AI opponent scaled to their ELO (attack speed, sprint speed,
 * strafing, crits, gapple usage). Bot matches never touch ELO or stats.
 *
 * <p>The bot is a silent, named, non-burning zombie driven by Paper's
 * {@code Mob#getPathfinder()} plus a small combat brain (strafe offsets, attack
 * cooldown with jitter, jump-crits, simulated gapple eats). It wears the
 * gamemode's preset kit so the fight reads like a real duel.</p>
 */
public class BotDuelManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String[] BOT_NAMES = {
            "Lemon", "Zesty", "Citrus", "Squeezy", "Peely", "Sour", "Juicy", "Pulp"
    };

    private final LemonPractice plugin;
    private final Map<UUID, BotDuel> activeByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, BotDuel> activeByBot = new ConcurrentHashMap<>();

    /** Difficulty derived from the player's ELO. */
    private record Difficulty(int attackDelayTicks, int attackJitterTicks, double speedMultiplier,
                              double strafeIntensity, int gapples, double critChance) {}

    private static final class BotDuel {
        final UUID playerUuid;
        final String gamemode;
        final Arena arena;
        final UUID botUuid;
        final Difficulty diff;
        BukkitTask aiTask;
        BukkitTask mirrorTask;
        /** Packet player-model mirrored onto the invisible zombie (v2), or null (v1 fallback). */
        com.lemonpvp.lemonpractice.replay.NpcReplayActor npc;
        boolean fighting;
        boolean ended;
        int attackCooldown;
        int eatingTicks;
        int gapplesLeft;
        int strafeTicks;
        double strafeSide = 1;

        BotDuel(UUID playerUuid, String gamemode, Arena arena, UUID botUuid, Difficulty diff) {
            this.playerUuid = playerUuid;
            this.gamemode = gamemode;
            this.arena = arena;
            this.botUuid = botUuid;
            this.diff = diff;
            this.gapplesLeft = diff.gapples();
        }
    }

    public BotDuelManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    public boolean isInBotDuel(UUID playerUuid) {
        return activeByPlayer.containsKey(playerUuid);
    }

    public boolean isBot(UUID entityUuid) {
        return activeByBot.containsKey(entityUuid);
    }

    /** The player a bot belongs to, or null. */
    public UUID ownerOfBot(UUID botUuid) {
        BotDuel d = activeByBot.get(botUuid);
        return d != null ? d.playerUuid : null;
    }

    // -----------------------------------------------------------------------
    // Start
    // -----------------------------------------------------------------------

    public void start(Player player, String gamemode) {
        if (player == null || !player.isOnline()) return;
        if (isInBotDuel(player.getUniqueId()) || plugin.getDuelManager().isInDuel(player.getUniqueId())) return;

        var arenaOpt = plugin.getArenaManager().getFreeArenaForGamemode(gamemode);
        if (arenaOpt.isEmpty()) {
            player.sendMessage(MM.deserialize("<red>No free arena for a bot match right now — re-queuing."));
            plugin.getQueueManager().addToQueue(player.getUniqueId(), gamemode);
            return;
        }
        Arena arena = arenaOpt.get();
        plugin.getArenaManager().markInUse(arena, true);

        int elo = plugin.getEloManager().getEloForMatchmaking(player.getUniqueId(), gamemode);
        Difficulty diff = difficultyFor(elo);

        // Prepare the player exactly like a real duel.
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        PlayerKit kit = plugin.getKitManager().getEffectiveKit(player.getUniqueId(), gamemode);
        if (kit != null) kit.getSlots().forEach((slot, item) -> player.getInventory().setItem(slot, item));

        Location spawn1 = arena.getSpawn1().clone().add(0, 2, 0);
        Location spawn2 = arena.getSpawn2().clone().add(0, 2, 0);
        player.teleport(spawn1);

        // Spawn and outfit the bot. With PacketEvents present (v2), the zombie
        // becomes an invisible physics/hitbox shell and a packet PLAYER model is
        // mirrored onto it for the duelist — the opponent sees a real player.
        String botName = BOT_NAMES[ThreadLocalRandom.current().nextInt(BOT_NAMES.length)]
                + (100 + ThreadLocalRandom.current().nextInt(900));
        boolean npcMode = Bukkit.getPluginManager().isPluginEnabled("packetevents");
        PlayerKit preset = plugin.getKitManager().getPresetKit(gamemode);
        Zombie bot = spawn2.getWorld().spawn(spawn2, Zombie.class, z -> {
            z.setAdult();
            z.setShouldBurnInDay(false);
            z.setSilent(true);
            z.setPersistent(true);
            z.setRemoveWhenFarAway(false);
            z.setCanPickupItems(false);
            z.setAI(false); // frozen during countdown
            var speedAttr = z.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(speedAttr.getBaseValue() * diff.speedMultiplier());
            if (npcMode) {
                // Invisible shell: no visible gear (it would float) — armor and
                // sword become attribute values; the NPC wears the visuals.
                z.setInvisible(true);
                applyCombatAttributes(z, preset);
            } else {
                z.customName(MM.deserialize("<gradient:#fffb00:#00ff00>" + botName + "</gradient> <gray>[BOT]"));
                z.setCustomNameVisible(true);
                equipBot(z, preset);
            }
        });

        BotDuel duel = new BotDuel(player.getUniqueId(), gamemode, arena, bot.getUniqueId(), diff);
        activeByPlayer.put(player.getUniqueId(), duel);
        activeByBot.put(bot.getUniqueId(), duel);

        if (npcMode) {
            try {
                var npc = new com.lemonpvp.lemonpractice.replay.NpcReplayActor(plugin, player, botName);
                npc.spawn(spawn2);
                if (preset != null) {
                    npc.equip(preset.getSlot(0),
                            preset.getSlot(KitManager.ARMOR_HEAD), preset.getSlot(KitManager.ARMOR_CHEST),
                            preset.getSlot(KitManager.ARMOR_LEGS), preset.getSlot(KitManager.ARMOR_FEET));
                }
                duel.npc = npc;
                // Mirror the shell every tick so the player model moves fluidly.
                duel.mirrorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    var e = Bukkit.getEntity(duel.botUuid);
                    if (e != null && e.isValid() && duel.npc != null) duel.npc.teleport(e.getLocation());
                }, 1L, 1L);
            } catch (Throwable t) {
                // Packet NPC failed — fall back to the visible zombie look.
                plugin.getLogger().warning("[BotDuel] NPC mirror failed, using zombie visuals: " + t);
                bot.setInvisible(false);
                bot.customName(MM.deserialize("<gradient:#fffb00:#00ff00>" + botName + "</gradient> <gray>[BOT]"));
                bot.setCustomNameVisible(true);
                equipBot(bot, preset);
            }
        }

        player.sendMessage(MM.deserialize("<gradient:#fffb00:#00ff00><bold>Bot Match</bold></gradient> "
                + "<gray>No opponent found — fighting <yellow>" + botName
                + " <gray>instead. <dark_gray>(does not affect your ELO)"));

        // 3-second countdown, then release the bot.
        final int[] count = {3};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (duel.ended) { task.cancel(); return; }
            Player p = Bukkit.getPlayer(duel.playerUuid);
            if (p == null || !p.isOnline()) { task.cancel(); end(duel, false, true); return; }
            if (count[0] > 0) {
                p.showTitle(Title.title(
                        Component.text(count[0], NamedTextColor.YELLOW), Component.empty(),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(700), Duration.ofMillis(200))));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.2f);
                count[0]--;
                return;
            }
            task.cancel();
            duel.fighting = true;
            if (bot.isValid()) bot.setAI(true);
            p.showTitle(Title.title(
                    Component.text("Fight!", NamedTextColor.GREEN), Component.empty(),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(800), Duration.ofMillis(200))));
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            startAi(duel);
        }, 20L, 20L);
    }

    /**
     * v2 (invisible shell): converts the preset's gear into attribute values so
     * combat feels identical without rendering floating armor — Prot-4 diamond
     * is ~20 armor / 8 toughness; the weapon becomes attack damage.
     */
    private void applyCombatAttributes(Zombie z, PlayerKit preset) {
        double armor = 20.0, toughness = 8.0, damage = 7.0;
        if (preset != null && preset.getSlot(KitManager.ARMOR_CHEST) != null) {
            String chest = preset.getSlot(KitManager.ARMOR_CHEST).getType().name();
            if (chest.startsWith("NETHERITE")) { armor = 20.0; toughness = 12.0; }
            else if (chest.startsWith("DIAMOND")) { armor = 20.0; toughness = 8.0; }
            else { armor = 15.0; toughness = 0.0; }
        }
        if (preset != null && preset.getSlot(0) != null) {
            String weapon = preset.getSlot(0).getType().name();
            if (weapon.contains("NETHERITE_SWORD")) damage = 8.0;
            else if (weapon.contains("DIAMOND_SWORD")) damage = 7.0;
            else if (weapon.contains("MACE")) damage = 6.0;
            else if (weapon.contains("TRIDENT")) damage = 9.0;
        }
        var a = z.getAttribute(Attribute.ARMOR);
        if (a != null) a.setBaseValue(armor);
        var t = z.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (t != null) t.setBaseValue(toughness);
        var d = z.getAttribute(Attribute.ATTACK_DAMAGE);
        if (d != null) d.setBaseValue(damage);
    }

    /** Mirrors damage flashes onto the packet player model. */
    public void notifyBotDamaged(UUID botUuid) {
        BotDuel duel = activeByBot.get(botUuid);
        if (duel != null && duel.npc != null) duel.npc.hurt();
    }

    private void equipBot(Zombie z, PlayerKit preset) {
        EntityEquipment eq = z.getEquipment();
        if (eq == null || preset == null) return;
        ItemStack weapon = preset.getSlot(0);
        if (weapon != null) eq.setItemInMainHand(weapon.clone());
        ItemStack head = preset.getSlot(KitManager.ARMOR_HEAD);
        ItemStack chest = preset.getSlot(KitManager.ARMOR_CHEST);
        ItemStack legs = preset.getSlot(KitManager.ARMOR_LEGS);
        ItemStack feet = preset.getSlot(KitManager.ARMOR_FEET);
        if (head != null) eq.setHelmet(head.clone());
        if (chest != null) eq.setChestplate(chest.clone());
        if (legs != null) eq.setLeggings(legs.clone());
        if (feet != null) eq.setBoots(feet.clone());
        eq.setItemInMainHandDropChance(0f);
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
    }

    /** Maps the player's ELO onto bot combat parameters. */
    private Difficulty difficultyFor(int elo) {
        if (elo < 900)   return new Difficulty(16, 4, 1.00, 0.6, 2, 0.05);
        if (elo < 1200)  return new Difficulty(13, 3, 1.12, 1.0, 3, 0.12);
        if (elo < 1500)  return new Difficulty(11, 2, 1.22, 1.4, 4, 0.20);
        return new Difficulty(9, 2, 1.32, 1.8, 5, 0.30);
    }

    // -----------------------------------------------------------------------
    // Combat brain
    // -----------------------------------------------------------------------

    private void startAi(BotDuel duel) {
        duel.aiTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(duel), 2L, 2L);
    }

    private void tick(BotDuel duel) {
        if (duel.ended) return;
        Player player = Bukkit.getPlayer(duel.playerUuid);
        var botEntity = Bukkit.getEntity(duel.botUuid);
        if (player == null || !player.isOnline()) { end(duel, false, true); return; }
        if (!(botEntity instanceof Zombie bot) || !bot.isValid() || bot.isDead()) { end(duel, true, false); return; }
        if (player.isDead()) return; // death event finishes the duel

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double distSq = bot.getLocation().distanceSquared(player.getLocation());

        // Simulated gapple: pause attacking ~1.6s, then absorb + regen.
        if (duel.eatingTicks > 0) {
            duel.eatingTicks -= 2;
            if (duel.eatingTicks <= 0) {
                bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0, false, true));
                bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, true));
            }
        } else if (duel.gapplesLeft > 0 && bot.getHealth() <= 8.0 && distSq > 9) {
            duel.gapplesLeft--;
            duel.eatingTicks = 32;
        }

        // Movement: chase with a perpendicular strafe offset so the approach
        // looks like a player circling, not a mob beeline.
        duel.strafeTicks -= 2;
        if (duel.strafeTicks <= 0) {
            duel.strafeTicks = 14 + rng.nextInt(16);
            duel.strafeSide = rng.nextBoolean() ? 1 : -1;
        }
        Location target = player.getLocation();
        if (distSq < 36) {
            Vector toPlayer = target.toVector().subtract(bot.getLocation().toVector()).setY(0);
            if (toPlayer.lengthSquared() > 0.01) {
                Vector side = toPlayer.clone().crossProduct(new Vector(0, 1, 0)).normalize()
                        .multiply(duel.strafeSide * duel.diff.strafeIntensity());
                target = target.clone().add(side);
            }
        }
        bot.getPathfinder().moveTo(target, 1.15);

        // Attack when in reach and off cooldown (skip while "eating").
        duel.attackCooldown -= 2;
        if (duel.eatingTicks <= 0 && distSq <= 10.5 && duel.attackCooldown <= 0) {
            duel.attackCooldown = duel.diff.attackDelayTicks()
                    + rng.nextInt(duel.diff.attackJitterTicks() + 1);
            bot.lookAt(player);
            if (rng.nextDouble() < duel.diff.critChance() && bot.isOnGround()) {
                bot.setVelocity(bot.getVelocity().add(new Vector(0, 0.32, 0))); // jump-crit
            }
            bot.swingMainHand();
            bot.attack(player);
            if (duel.npc != null) duel.npc.swing();
        }
    }

    // -----------------------------------------------------------------------
    // End / cleanup
    // -----------------------------------------------------------------------

    /** Ends the duel. {@code silent} skips titles (quit/shutdown paths). */
    public void end(BotDuel duel, boolean playerWon, boolean silent) {
        if (duel.ended) return;
        duel.ended = true;
        if (duel.aiTask != null) duel.aiTask.cancel();
        if (duel.mirrorTask != null) duel.mirrorTask.cancel();
        if (duel.npc != null) {
            try { duel.npc.remove(); } catch (Throwable ignored) {}
        }
        activeByPlayer.remove(duel.playerUuid);
        activeByBot.remove(duel.botUuid);

        var botEntity = Bukkit.getEntity(duel.botUuid);
        if (botEntity != null) botEntity.remove();

        Player player = Bukkit.getPlayer(duel.playerUuid);
        if (!silent && player != null && player.isOnline()) {
            if (playerWon) {
                player.showTitle(Title.title(
                        Component.text("Victory!", NamedTextColor.GOLD), Component.empty(),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
                tryPlayWinEffect(player);
            } else {
                player.showTitle(Title.title(
                        Component.text("Defeat!", NamedTextColor.RED), Component.empty(),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));
            }
            player.sendMessage(MM.deserialize("<gray>Bot match over — <dark_gray>no ELO changes."));
        }

        // Return to lobby, then free + reset the arena after the player is out.
        final UUID uuid = duel.playerUuid;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead()) {
                String lobbyServer = plugin.getServersConfig().getString("servers.lobby.name", "lobby");
                plugin.getVelocityMessaging().sendToServer(p, lobbyServer);
            }
        }, 60L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getArenaManager().markInUse(duel.arena, false);
            plugin.getArenaManager().resetArena(duel.arena);
        }, 80L);
    }

    /** End via the player uuid (death/quit listeners). */
    public void endForPlayer(UUID playerUuid, boolean playerWon, boolean silent) {
        BotDuel duel = activeByPlayer.get(playerUuid);
        if (duel != null) end(duel, playerWon, silent);
    }

    /** End via the bot uuid (bot died). */
    public void endForBot(UUID botUuid, boolean playerWon) {
        BotDuel duel = activeByBot.get(botUuid);
        if (duel != null) end(duel, playerWon, false);
    }

    /** Cleans up every bot duel (plugin disable). */
    public void shutdown() {
        for (BotDuel duel : activeByPlayer.values().toArray(new BotDuel[0])) {
            end(duel, false, true);
        }
    }

    private void tryPlayWinEffect(Player winner) {
        try {
            org.bukkit.plugin.Plugin lc = Bukkit.getPluginManager().getPlugin("LemonCosmetics");
            if (lc != null && lc.isEnabled()
                    && lc instanceof com.lemonpvp.lemoncosmetics.LemonCosmetics cosmetics) {
                cosmetics.getWinEffectManager().play(winner);
            }
        } catch (Throwable ignored) {
            // cosmetics optional
        }
    }
}

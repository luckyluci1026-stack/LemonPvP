package com.lemonpvp.lemonevents.game.horror;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class HuntGame extends AbstractGame {

    private final int gameDuration;
    private final int hunterSpeedLevel;
    private final int fogLevel;

    private UUID currentHunter;
    private final Map<UUID, Integer> hunterHits = new HashMap<>(); // how many times each prey has hit the hunter
    private int timeLeft;

    public HuntGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
        this.gameDuration  = plugin.getEventsConfig().getInt("horror.HORROR_Hunt.game-duration", 300);
        this.hunterSpeedLevel = plugin.getEventsConfig().getInt("horror.HORROR_Hunt.hunter-speed-level", 1);
        this.fogLevel       = plugin.getEventsConfig().getInt("horror.HORROR_Hunt.fog-level", 1);
    }

    @Override
    public void startGame() {
        running = true;
        timeLeft = gameDuration;

        selectHunter();
        spawnPlayers();
        applyEffects();

        broadcastParticipants(MM.deserialize(
            "<bold><dark_red>HORROR: The Hunt</dark_red></bold>"));
        broadcastParticipants(MM.deserialize(
            "<red>🏃 " + Bukkit.getOfflinePlayer(currentHunter).getName() + " is the Hunter!</red>"));
        broadcastParticipants(MM.deserialize(
            "<gray>Prey: survive " + (gameDuration / 60) + " minutes OR hit the hunter 3 times to switch!"));

        // Timer countdown
        scheduleTask(Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                if (!running) { cancel(); return; }
                timeLeft--;
                if (timeLeft == 60) broadcastParticipants(MM.deserialize("<yellow>1 minute left! Survive!"));
                if (timeLeft <= 0) {
                    // Prey wins — all surviving non-hunters win
                    broadcastAll(MM.deserialize(
                        "<green><bold>🏃 The Prey survived! They win!</bold></green>"));
                    List<UUID> prey = new ArrayList<>(participants);
                    prey.remove(currentHunter);
                    prey.forEach(u -> { participants.remove(u); finishOrder.add(0, u); });
                    participants.remove(currentHunter);
                    finishOrder.add(0, currentHunter);
                    Collections.reverse(finishOrder);
                    endGame();
                    cancel();
                }
            }
        }, 20L, 20L));
    }

    private void selectHunter() {
        List<UUID> list = new ArrayList<>(participants);
        currentHunter = list.get(new Random().nextInt(list.size()));
    }

    private void spawnPlayers() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) return;

        Player hunter = Bukkit.getPlayer(currentHunter);
        if (hunter != null) hunter.teleport(new Location(world, 0.5, 65, 0.5));

        Random rng = new Random();
        for (UUID uuid : participants) {
            if (uuid.equals(currentHunter)) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            double angle = rng.nextDouble() * 2 * Math.PI;
            double r = 20 + rng.nextInt(20);
            p.teleport(new Location(world, Math.cos(angle) * r, 65, Math.sin(angle) * r));
        }
    }

    private void applyEffects() {
        Player hunter = Bukkit.getPlayer(currentHunter);
        if (hunter != null) {
            hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, hunterSpeedLevel));
            hunter.getInventory().clear();
            hunter.setGameMode(GameMode.SURVIVAL);
            hunter.sendMessage(MM.deserialize("<red>You are the Hunter! Chase them down!"));
        }

        for (UUID uuid : participants) {
            if (uuid.equals(currentHunter)) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, fogLevel - 1, false, false));
            p.getInventory().clear();
            // Give wooden sword for counter-attack
            p.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
            p.setGameMode(GameMode.SURVIVAL);
            p.sendMessage(MM.deserialize("<gray>Survive! Hit the hunter 3 times to become the new Hunter."));
        }
    }

    /** Called by EventPlayerListener when prey hits the hunter. */
    public void onHunterHit(Player prey, Player hunter) {
        if (!running) return;
        if (!hunter.getUniqueId().equals(currentHunter)) return;
        if (!participants.contains(prey.getUniqueId())) return;

        int hits = hunterHits.merge(prey.getUniqueId(), 1, Integer::sum);
        broadcastParticipants(MM.deserialize(
            "<yellow>" + prey.getName() + " hit the Hunter! (" + hits + "/3)"));

        if (hits >= 3) {
            // Switch hunter
            Player oldHunter = Bukkit.getPlayer(currentHunter);
            if (oldHunter != null) {
                oldHunter.removePotionEffect(PotionEffectType.SPEED);
                oldHunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, fogLevel - 1, false, false));
                oldHunter.getInventory().clear();
                oldHunter.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
            }
            currentHunter = prey.getUniqueId();
            hunterHits.clear();
            applyNewHunter(prey);
            broadcastParticipants(MM.deserialize(
                "<red>🔄 <yellow>" + prey.getName() + "</yellow> is now the Hunter!"));
        }
    }

    private void applyNewHunter(Player newHunter) {
        newHunter.removePotionEffect(PotionEffectType.BLINDNESS);
        newHunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, hunterSpeedLevel));
        newHunter.getInventory().clear();
    }

    @Override
    protected void doCleanup() {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }
}

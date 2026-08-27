package com.lemonpvp.lemonevents.game.horror;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class MafiaGame extends AbstractGame {

    private enum Phase { NIGHT, DAY }
    private enum Role  { MAFIA, VILLAGER }

    private final int mafiaCount;
    private final int nightDuration;
    private final int dayDuration;

    private final Map<UUID, Role>    roles   = new HashMap<>();
    private final Map<UUID, Integer> votes   = new HashMap<>();
    // Mafia night kill votes
    private final Map<UUID, UUID>    mafiaVotes = new HashMap<>();

    private Phase currentPhase = Phase.NIGHT;
    private int   phaseTimer;

    public MafiaGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
        this.mafiaCount   = plugin.getEventsConfig().getInt("horror.HORROR_Mafia.mafia-count", 2);
        this.nightDuration = plugin.getEventsConfig().getInt("horror.HORROR_Mafia.night-duration", 60);
        this.dayDuration   = plugin.getEventsConfig().getInt("horror.HORROR_Mafia.day-duration", 120);
    }

    @Override
    public void startGame() {
        running = true;

        broadcastParticipants(MM.deserialize(
            "<bold><dark_red>HORROR: Mafia</dark_red></bold> " +
            "<red>Roles have been assigned secretly. Survive the night!</red>"));

        assignRoles();
        sendRoleMessages();
        setupPlayers();

        startPhase(Phase.NIGHT);
    }

    private void assignRoles() {
        List<UUID> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled);
        int count = 0;
        for (UUID uuid : shuffled) {
            roles.put(uuid, count++ < mafiaCount ? Role.MAFIA : Role.VILLAGER);
        }
    }

    private void sendRoleMessages() {
        List<UUID> mafiaList = getRoleList(Role.MAFIA);
        String mafiaNames = mafiaList.stream()
                .map(u -> Bukkit.getOfflinePlayer(u).getName())
                .collect(Collectors.joining(", "));

        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            Role role = roles.get(uuid);
            if (role == Role.MAFIA) {
                p.sendMessage(MM.deserialize("<dark_red><bold>Your role: MAFIA</bold></dark_red>"));
                p.sendMessage(MM.deserialize("<red>Your allies: <yellow>" + mafiaNames + "</yellow>"));
                p.sendMessage(MM.deserialize("<gray>At night: vote to kill a Villager with <yellow>/mafiakill <player></yellow>"));
            } else {
                p.sendMessage(MM.deserialize("<green><bold>Your role: VILLAGER</bold></green>"));
                p.sendMessage(MM.deserialize("<gray>During the day: vote to eliminate the Mafia with <yellow>/mafiaelim <player></yellow>"));
            }
        }
    }

    private void setupPlayers() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (world != null) p.teleport(new Location(world, 0.5, 65, 0.5));
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();

            // Give "Evidence" item to villagers — vanilla paper, no texturepack
            if (roles.get(uuid) == Role.VILLAGER) {
                ItemStack evidence = new ItemStack(Material.PAPER);
                var meta = evidence.getItemMeta();
                if (meta != null) {
                    meta.displayName(MM.deserialize("<red>Evidence</red>"));
                    meta.lore(List.of(MM.deserialize("<gray>Find the Mafia. Vote them out.")));
                    evidence.setItemMeta(meta);
                }
                p.getInventory().setItem(0, evidence);
            }
        }
    }

    private void startPhase(Phase phase) {
        currentPhase = phase;
        votes.clear();
        mafiaVotes.clear();
        phaseTimer = phase == Phase.NIGHT ? nightDuration : dayDuration;

        if (phase == Phase.NIGHT) {
            broadcastParticipants(MM.deserialize("<dark_gray><bold>🌙 NIGHT has fallen...</bold></dark_gray>"));
            broadcastParticipants(MM.deserialize("<gray>Mafia: use <yellow>/mafiakill <player></yellow> to choose your target."));
            // Apply blindness to Villagers during night
            for (UUID uuid : participants) {
                if (roles.get(uuid) == Role.VILLAGER) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, nightDuration * 20, 0));
                }
            }
        } else {
            broadcastParticipants(MM.deserialize("<yellow><bold>☀ DAY has begun!</bold></yellow>"));
            broadcastParticipants(MM.deserialize("<gray>Vote to eliminate: <yellow>/mafiaelim <player></yellow>"));
            for (UUID uuid : participants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            }
        }

        scheduleTask(new BukkitRunnable() {
            int t = phaseTimer;
            @Override public void run() {
                if (!running) { cancel(); return; }
                t--;
                if (t == 10) broadcastParticipants(MM.deserialize("<yellow>10 seconds left in this phase!"));
                if (t <= 0) { phaseEnd(); cancel(); }
            }
        }.runTaskTimer(plugin, 20L, 20L));
    }

    private void phaseEnd() {
        if (currentPhase == Phase.NIGHT) {
            resolveNightKill();
        } else {
            resolveDayVote();
        }
    }

    private void resolveNightKill() {
        // Find most-voted villager by mafia
        UUID target = mostVoted(mafiaVotes.values().stream().collect(
                Collectors.groupingBy(u -> u, Collectors.counting())));
        if (target != null && participants.contains(target)) {
            String targetName = Bukkit.getOfflinePlayer(target).getName();
            broadcastParticipants(MM.deserialize("<red>💀 " + targetName + " was killed in the night!"));
            eliminate(target, null);
        } else {
            broadcastParticipants(MM.deserialize("<gray>The Mafia couldn't agree on a target tonight."));
        }
        if (running) startPhase(Phase.DAY);
    }

    private void resolveDayVote() {
        Map<UUID, Long> tally = votes.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingLong(e -> e.getValue())));
        UUID target = mostVoted(tally);
        if (target != null && participants.contains(target)) {
            String targetName = Bukkit.getOfflinePlayer(target).getName();
            Role role = roles.get(target);
            broadcastParticipants(MM.deserialize("<yellow>🗳 The town voted to eliminate <white>" + targetName + "</white>!"));
            broadcastParticipants(MM.deserialize("<gray>They were... <bold>" + role + "</bold>."));
            eliminate(target, null);
        } else {
            broadcastParticipants(MM.deserialize("<gray>The town couldn't reach a verdict."));
        }
        if (running) startPhase(Phase.NIGHT);
    }

    /** Called by listener when a mafia player does /mafiakill. */
    public void mafiaVote(UUID voter, UUID target) {
        if (!running || currentPhase != Phase.NIGHT) return;
        if (roles.get(voter) != Role.MAFIA) return;
        if (!participants.contains(target)) return;
        mafiaVotes.put(voter, target);
        Player voter0 = Bukkit.getPlayer(voter);
        if (voter0 != null) voter0.sendMessage(MM.deserialize("<green>Vote registered for <yellow>" +
                Bukkit.getOfflinePlayer(target).getName() + "</yellow>."));
    }

    /** Called by listener when any player does /mafiaelim. */
    public void dayVote(UUID voter, UUID target) {
        if (!running || currentPhase != Phase.DAY) return;
        if (!participants.contains(voter)) return;
        votes.put(target, votes.getOrDefault(target, 0) + 1);
        broadcastParticipants(MM.deserialize("<gray>" + Bukkit.getOfflinePlayer(voter).getName() +
                " voted to eliminate <yellow>" + Bukkit.getOfflinePlayer(target).getName() + "</yellow>."));
    }

    @Override
    protected void checkWinCondition() {
        long mafiaAlive = participants.stream().filter(u -> roles.getOrDefault(u, Role.VILLAGER) == Role.MAFIA).count();
        long villagersAlive = participants.stream().filter(u -> roles.getOrDefault(u, Role.VILLAGER) == Role.VILLAGER).count();
        if (mafiaAlive == 0) {
            broadcastAll(MM.deserialize("<green><bold>🏘 Villagers win! The Mafia has been eliminated!</bold></green>"));
            List<UUID> mafiaList = getRoleList(Role.MAFIA);
            List<UUID> villagerList = getRoleList(Role.VILLAGER);
            participants.removeAll(mafiaList);
            participants.removeAll(villagerList);
            // Prepend losers first, winners last — the prepend convention already
            // puts index 0 = 1st place. (The old trailing reverse flipped the list
            // and handed 1st place to the first player eliminated.)
            mafiaList.forEach(u -> finishOrder.add(0, u));
            villagerList.forEach(u -> finishOrder.add(0, u));
            endGame();
        } else if (mafiaAlive >= villagersAlive) {
            broadcastAll(MM.deserialize("<dark_red><bold>😈 Mafia wins! They have taken over the village!</bold></dark_red>"));
            // Mafia wins: villagers prepended first (bottom), mafia last (top).
            List<UUID> mafiaList = getRoleList(Role.MAFIA);
            List<UUID> villagerList = getRoleList(Role.VILLAGER);
            participants.removeAll(mafiaList);
            participants.removeAll(villagerList);
            villagerList.forEach(u -> finishOrder.add(0, u));
            mafiaList.forEach(u -> finishOrder.add(0, u));
            endGame();
        }
    }

    private List<UUID> getRoleList(Role role) {
        return roles.entrySet().stream()
                .filter(e -> e.getValue() == role && participants.contains(e.getKey()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private <T> UUID mostVoted(Map<T, Long> tally) {
        return tally.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> (UUID) e.getKey()).orElse(null);
    }

    private UUID mostVoted(Collection<UUID> values) {
        Map<UUID, Long> freq = values.stream().collect(Collectors.groupingBy(u -> u, Collectors.counting()));
        return mostVoted(freq);
    }

    @Override
    protected void doCleanup() {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
        }
    }
}

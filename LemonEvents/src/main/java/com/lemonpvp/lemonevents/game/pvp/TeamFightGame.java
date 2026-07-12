package com.lemonpvp.lemonevents.game.pvp;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;

import java.util.*;

public class TeamFightGame extends AbstractGame {

    private static final NamedTextColor TEAM_A_COLOR = NamedTextColor.RED;
    private static final NamedTextColor TEAM_B_COLOR = NamedTextColor.BLUE;

    private final Set<UUID> teamA = new LinkedHashSet<>();
    private final Set<UUID> teamB = new LinkedHashSet<>();
    private Scoreboard scoreboard;
    private Team sbTeamA;
    private Team sbTeamB;

    public TeamFightGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    public void startGame() {
        running = true;
        assignTeams();
        setupScoreboard();

        broadcastParticipants(MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>Team Fight</gradient></bold> <green>starting!</green>"));
        broadcastParticipants(MM.deserialize(
            "<red>Team A</red>: " + teamNames(teamA) + " | <blue>Team B</blue>: " + teamNames(teamB)));

        spawnTeams();
    }

    private void assignTeams() {
        List<UUID> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled);
        for (int i = 0; i < shuffled.size(); i++) {
            if (i % 2 == 0) teamA.add(shuffled.get(i));
            else teamB.add(shuffled.get(i));
        }
    }

    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        sbTeamA = scoreboard.registerNewTeam("team_a");
        sbTeamA.color(TEAM_A_COLOR);
        sbTeamA.prefix(net.kyori.adventure.text.Component.text("[A] ", TEAM_A_COLOR));

        sbTeamB = scoreboard.registerNewTeam("team_b");
        sbTeamB.color(TEAM_B_COLOR);
        sbTeamB.prefix(net.kyori.adventure.text.Component.text("[B] ", TEAM_B_COLOR));

        for (UUID uuid : teamA) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) { sbTeamA.addPlayer(p); p.setScoreboard(scoreboard); }
        }
        for (UUID uuid : teamB) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) { sbTeamB.addPlayer(p); p.setScoreboard(scoreboard); }
        }
    }

    private void spawnTeams() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) return;

        for (UUID uuid : teamA) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(new Location(world, -20.5, 65, 0.5));
                setupKit(p, Material.RED_WOOL);
            }
        }
        for (UUID uuid : teamB) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(new Location(world, 20.5, 65, 0.5));
                setupKit(p, Material.BLUE_WOOL);
            }
        }
    }

    private void setupKit(Player player, Material teamColor) {
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));
        player.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
    }

    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasA = teamA.remove(uuid);
        boolean wasB = teamB.remove(uuid);
        if (!wasA && !wasB) return;

        player.setGameMode(GameMode.SPECTATOR);
        broadcastParticipants(MM.deserialize(
            "<gray>" + player.getName() + " was eliminated!"));
        broadcastParticipants(MM.deserialize(
            "<red>A:" + teamA.size() + "</red> vs <blue>B:" + teamB.size() + "</blue>"));

        // Winners prepended last land at index 0 = 1st place (dead players were
        // already prepended when eliminated). The old trailing reverse flipped
        // the list, awarding 1st place to the first player eliminated.
        if (teamA.isEmpty()) {
            broadcastAll(MM.deserialize("<blue><bold>Team B wins!</bold></blue>"));
            new ArrayList<>(teamB).forEach(w -> { participants.remove(w); finishOrder.add(0, w); });
            endGame();
        } else if (teamB.isEmpty()) {
            broadcastAll(MM.deserialize("<red><bold>Team A wins!</bold></red>"));
            new ArrayList<>(teamA).forEach(w -> { participants.remove(w); finishOrder.add(0, w); });
            endGame();
        }
    }

    @Override
    public void handleQuit(UUID uuid) {
        if (!participants.remove(uuid)) return;
        finishOrder.add(0, uuid);
        // Treat disconnect as elimination so team win-condition is re-evaluated
        teamA.remove(uuid);
        teamB.remove(uuid);
        // Reuse onPlayerDeath win-check path (player ref can be null — only team emptiness matters)
        Player ghost = Bukkit.getPlayer(uuid);
        if (ghost != null) ghost.setGameMode(GameMode.SPECTATOR);
        broadcastParticipants(MM.deserialize("<gray>" + Bukkit.getOfflinePlayer(uuid).getName() + " disconnected!"));
        if (teamA.isEmpty()) {
            broadcastAll(MM.deserialize("<blue><bold>Team B wins!</bold></blue>"));
            new ArrayList<>(teamB).forEach(w -> { participants.remove(w); finishOrder.add(0, w); });
            endGame();
        } else if (teamB.isEmpty()) {
            broadcastAll(MM.deserialize("<red><bold>Team A wins!</bold></red>"));
            new ArrayList<>(teamA).forEach(w -> { participants.remove(w); finishOrder.add(0, w); });
            endGame();
        }
    }

    @Override
    protected void checkWinCondition() {} // handled by onPlayerDeath

    private String teamNames(Set<UUID> team) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : team) {
            String n = Bukkit.getOfflinePlayer(uuid).getName();
            if (n != null) sb.append(n).append(", ");
        }
        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "none";
    }

    @Override
    protected void doCleanup() {
        for (UUID uuid : teamA) { Player p = Bukkit.getPlayer(uuid); if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); }
        for (UUID uuid : teamB) { Player p = Bukkit.getPlayer(uuid); if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); }
    }
}

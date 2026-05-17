package com.lemonpvp.lemoncore.scoreboard;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

public class ScoreboardManager {

    private final LemonCore plugin;

    public ScoreboardManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().getCached(p.getUniqueId());
                if (data == null) continue;
                Bukkit.getScheduler().runTask(plugin, () -> updateScoreboard(p, data));
            }
        }, 20L, 20L);
    }

    public void updateScoreboard(Player player, PlayerData data) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        org.bukkit.scoreboard.ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard sb = sbm.getNewScoreboard();

        String titleRaw = plugin.getConfig().getString("scoreboard.title",
                "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold>");
        Objective obj = sb.registerNewObjective("lemoncore", Criteria.DUMMY, TextUtil.parse(titleRaw));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int score = lines.size() + 1;
        for (String line : lines) {
            score--;
            String formatted = line
                    .replace("{kills}", String.valueOf(data.getKills()))
                    .replace("{killstreak}", String.valueOf(data.getKillstreak()))
                    .replace("{coins}", String.valueOf(data.getCoins()))
                    .replace("{deaths}", String.valueOf(data.getDeaths()))
                    .replace("{player}", data.getUsername());

            // Use unique team entries for each line
            String entryKey = "lc_line_" + score;
            Team team = sb.registerNewTeam(entryKey);
            team.prefix(TextUtil.parse(formatted));
            team.addEntry(entryKey);
            obj.getScore(entryKey).setScore(score);
        }

        player.setScoreboard(sb);
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}

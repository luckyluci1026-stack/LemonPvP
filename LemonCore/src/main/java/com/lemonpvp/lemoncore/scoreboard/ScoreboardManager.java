package com.lemonpvp.lemoncore.scoreboard;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

public class ScoreboardManager {

    private final LemonCore plugin;
    private final boolean hasPapi;

    public ScoreboardManager(LemonCore plugin) {
        this.plugin = plugin;
        this.hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public void startUpdating() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().getCached(p.getUniqueId());
                if (data == null) continue;
                updateScoreboard(p, data);
            }
        }, 20L, 20L);
    }

    public void updateScoreboard(Player player, PlayerData data) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        org.bukkit.scoreboard.ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard sb = sbm.getNewScoreboard();

        String titleRaw = plugin.getConfig().getString("scoreboard.title",
                "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold>");
        Objective obj = sb.registerNewObjective("lemoncore", Criteria.DUMMY,
                TextUtil.parse(titleRaw));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        for (int i = 0; i < lines.size(); i++) {
            int scoreValue = lines.size() - i;
            String line = lines.get(i);

            // Resolve PlaceholderAPI placeholders first
            if (hasPapi) {
                line = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, line);
            }

            // Manual internal replacements
            line = line
                    .replace("{kills}", String.valueOf(data.getKills()))
                    .replace("{killstreak}", String.valueOf(data.getKillstreak()))
                    .replace("{coins}", String.valueOf(data.getCoins()))
                    .replace("{deaths}", String.valueOf(data.getDeaths()))
                    .replace("{player}", data.getUsername());

            // Use increasing-spaces as unique but invisible entry names.
            // Score.customName() (Paper 1.20.4+) controls what is actually rendered.
            String entryKey = " ".repeat(i + 1);
            Score score = obj.getScore(entryKey);
            score.setScore(scoreValue);
            score.customName(TextUtil.parse(line));
        }

        player.setScoreboard(sb);
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}

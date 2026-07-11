package com.lemonpvp.lemoncore.scoreboard;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

public class ScoreboardManager {

    /** Handles both &-codes and §-codes used by LuckPerms prefixes. */
    private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY =
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                    .character('&').hexColors().build();

    private final LemonCore plugin;
    private final boolean hasPapi;

    public ScoreboardManager(LemonCore plugin) {
        this.plugin = plugin;
        this.hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Resolves the player's rank for the {rank} placeholder: the LuckPerms
     * prefix (converted from legacy &-codes to MiniMessage so it survives
     * TextUtil.parse), falling back to the capitalised primary group. Returns
     * null when LuckPerms is absent or has no data yet.
     */
    private String resolveRank(Player player) {
        try {
            net.luckperms.api.LuckPerms lp = plugin.getLuckPerms();
            if (lp == null) return null;
            var adapter = lp.getPlayerAdapter(Player.class);
            String prefix = adapter.getMetaData(player).getPrefix();
            if (prefix != null && !prefix.isBlank()) {
                return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .serialize(LEGACY.deserialize(prefix.replace('§', '&').trim()));
            }
            var user = adapter.getUser(player);
            String group = user != null ? user.getPrimaryGroup() : null;
            if (group == null || group.isBlank()) return null;
            return "<white>" + Character.toUpperCase(group.charAt(0)) + group.substring(1);
        } catch (Throwable t) {
            return null; // LuckPerms missing/incompatible — placeholder falls back
        }
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

        // Yield to match scoreboards (LemonPractice duel/FFA) so we don't flicker
        // over them every tick. Those objectives are named "duel" / "ffa".
        Objective current = player.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
        if (current != null) {
            String name = current.getName();
            if (name.equals("duel") || name.equals("ffa")) return;
        }

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

            // Manual internal replacements. {rank}: the ELO badge pushed by
            // LemonPractice when present, otherwise the LuckPerms rank prefix
            // (nothing ever populated rankDisplay on lobby servers, so the
            // placeholder used to always render the fallback star).
            String rank = data.getRankDisplay();
            if (rank == null || rank.isBlank()) rank = resolveRank(player);
            if (rank == null || rank.isBlank()) rank = "<gray>Player";

            line = line
                    .replace("{kills}", String.valueOf(data.getKills()))
                    .replace("{killstreak}", String.valueOf(data.getKillstreak()))
                    .replace("{best_killstreak}", String.valueOf(data.getBestKillstreak()))
                    .replace("{coins}", String.valueOf(data.getCoins()))
                    .replace("{coins_short}", com.lemonpvp.lemoncore.util.TextUtil.formatCoins(data.getCoins()))
                    .replace("{deaths}", String.valueOf(data.getDeaths()))
                    .replace("{kdr}", String.format("%.2f", data.getKDRatio()))
                    .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("{player}", data.getUsername())
                    .replace("{rank}", rank)
                    .replace("{apples}", String.valueOf(data.getApples()))
                    .replace("{apples_short}", com.lemonpvp.lemoncore.util.TextUtil.formatCoins(data.getApples()))
                    .replace("{planks}", String.valueOf(data.getPlanks()))
                    .replace("{planks_short}", com.lemonpvp.lemoncore.util.TextUtil.formatCoins(data.getPlanks()));

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

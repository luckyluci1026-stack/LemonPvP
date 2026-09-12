/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.config.ConfigReloadable;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code bans.yml} ladder: run a command when a player's <i>total</i> flag
 * count crosses a threshold.
 *
 * <p>{@link PunishmentManager} counts violations separately inside each
 * punishment group, so a player sitting at 30 Reach flags and 30 Knockback
 * flags never reaches a 50-flag rung in either group. This counts every flag
 * from every check into one running total, which is the number operators mean
 * when they say "he has 50 flags". Both run at the same time and do not
 * interfere — one reacts to a specific check, the other to overall
 * suspicion.</p>
 *
 * <p>Two safeguards keep the simple total from being trigger-happy. Flags age
 * out after {@code remember-flags-for} seconds, and no rung fires until the
 * player has tripped at least {@code min-different-checks} distinct checks — a
 * real cheat client trips several, while one check firing repeatedly is far
 * more often a false positive or a lag artifact.</p>
 *
 * <p>State lives on the {@link GrimPlayer}, so it is per connection: a rung
 * fires at most once per session, and everything resets on reconnect.</p>
 */
public class BanLadder implements ConfigReloadable {

    private final GrimPlayer player;

    private boolean enabled;
    private long rememberForMillis;
    private int minDifferentChecks;
    private String alertFormat = "";
    private final List<String> ignoredChecks = new ArrayList<>();
    /** Ascending by threshold. */
    private final List<Rung> ladder = new ArrayList<>();

    private final ArrayDeque<FlagEntry> flags = new ArrayDeque<>();
    /** Highest rung already fired this session; rungs never repeat. */
    private int firedThreshold;

    public BanLadder(GrimPlayer player) {
        this.player = player;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void reload(ConfigManager config) {
        enabled = config.getBooleanElse("bans.enabled", true);
        rememberForMillis = Math.max(0L, config.getIntElse("bans.remember-flags-for", 900)) * 1000L;
        minDifferentChecks = Math.max(1, config.getIntElse("bans.min-different-checks", 2));
        alertFormat = config.getStringElse("bans.alert-format",
                "%prefix% &f%player% &7is at &c%flags% &7flags across &c%checks% &7checks");

        ignoredChecks.clear();
        for (String name : config.getStringListElse("bans.ignore-checks", new ArrayList<>())) {
            if (name != null && !name.isBlank()) ignoredChecks.add(name.toLowerCase(Locale.ROOT));
        }

        ladder.clear();
        try {
            for (Object entry : config.getStringListElse("bans.ladder", new ArrayList<>())) {
                if (!(entry instanceof LinkedHashMap)) continue;
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) entry;

                Object rawFlags = map.get("flags");
                Object rawCommand = map.get("command");
                if (!(rawFlags instanceof Number threshold) || rawCommand == null) continue;

                String command = rawCommand.toString();
                if (command.isBlank()) continue;
                ladder.add(new Rung(threshold.intValue(), command));
            }
            ladder.sort((a, b) -> Integer.compare(a.threshold(), b.threshold()));
        } catch (Exception e) {
            LogUtil.error("Error while loading bans.yml! This is likely your fault!", e);
            ladder.clear();
        }

        // Thresholds may have moved under the operator's feet on a reload; let
        // the ladder re-evaluate from scratch rather than silently skipping
        // rungs that are now below where the player already stands.
        firedThreshold = 0;
    }

    /** Called for every flag the player collects, from {@link PunishmentManager}. */
    public void onFlag(Check check) {
        if (!enabled || ladder.isEmpty()) return;

        String checkName = check.getCheckName();
        if (checkName == null || isIgnored(checkName)) return;

        long now = System.currentTimeMillis();
        flags.addLast(new FlagEntry(now, checkName));
        evict(now);

        int total = flags.size();
        if (total <= firedThreshold) return; // nothing new can fire
        if (firedThreshold >= ladder.get(ladder.size() - 1).threshold()) return; // whole ladder spent

        // Tally once; both the safeguard and the placeholders need it.
        Map<String, Integer> perCheck = new HashMap<>();
        for (FlagEntry entry : flags) {
            perCheck.merge(entry.checkName(), 1, Integer::sum);
        }
        if (perCheck.size() < minDifferentChecks) return;

        String topCheck = "";
        int topCount = 0;
        for (Map.Entry<String, Integer> entry : perCheck.entrySet()) {
            if (entry.getValue() > topCount) {
                topCount = entry.getValue();
                topCheck = entry.getKey();
            }
        }

        for (Rung rung : ladder) {
            if (rung.threshold() > total) break;
            if (rung.threshold() <= firedThreshold) continue;
            firedThreshold = rung.threshold();
            run(rung.command(), total, perCheck.size(), topCheck, topCount);
        }
    }

    private void evict(long now) {
        if (rememberForMillis <= 0) return; // 0 = remember for the whole session
        while (!flags.isEmpty() && now - flags.peekFirst().time() > rememberForMillis) {
            flags.pollFirst();
        }
    }

    private boolean isIgnored(String checkName) {
        String lower = checkName.toLowerCase(Locale.ROOT);
        for (String ignored : ignoredChecks) {
            // Same substring matching punishments.yml uses, so "BadPackets"
            // covers BadPacketsA, BadPacketsB, ...
            if (lower.contains(ignored)) return true;
        }
        return false;
    }

    private void run(String rawCommand, int total, int checkCount, String topCheck, int topCount) {
        String command = MessageUtil.replacePlaceholders(player, rawCommand
                .replace("%flags%", Integer.toString(total))
                .replace("%checks%", Integer.toString(checkCount))
                .replace("%top_check%", topCheck)
                .replace("%top_count%", Integer.toString(topCount)));

        if (command.equals("[alert]")) {
            String message = MessageUtil.replacePlaceholders(player, alertFormat
                    .replace("%flags%", Integer.toString(total))
                    .replace("%checks%", Integer.toString(checkCount))
                    .replace("%top_check%", topCheck)
                    .replace("%top_count%", Integer.toString(topCount)));
            GrimAPI.INSTANCE.getAlertManager().sendAlert(MessageUtil.miniMessage(message), null);
            return;
        }

        // Commands must run on the server thread, never on the packet thread.
        // Same reporting as PunishmentManager: a ban that silently resolves to
        // nothing is worse than no ban at all, because nobody notices.
        GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(
                GrimAPI.INSTANCE.getGrimPlugin(), () -> dispatch(command));
    }

    private void dispatch(String command) {
        boolean resolved = GrimAPI.INSTANCE.getPlatformServer().dispatchCommandChecked(
                GrimAPI.INSTANCE.getPlatformServer().getConsoleSender(), command);

        if (resolved) {
            LogUtil.info("bans.yml rung for " + player.user.getName() + ": " + command);
            return;
        }

        LogUtil.warn("bans.yml rung for " + player.user.getName()
                + " did NOT run — this server has no such command: " + command);
        LogUtil.warn("Nothing happened to that player. If your ban plugin runs on the proxy, it must also be "
                + "installed on this backend server (sharing the same database) for bans.yml to reach it.");
    }

    private record FlagEntry(long time, String checkName) {
    }

    private record Rung(int threshold, String command) {
    }
}

package com.lemonpvp.lemonnametags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the lifecycle of every player's over-head Text Display nametag.
 *
 * <p>Each tag is a {@link TextDisplay} mounted as a passenger of the player, so
 * it follows them smoothly (no per-tick teleport / packet spam). The wearer is
 * hidden from their own tag; everyone else sees it. Text is rebuilt only on
 * (re)create and on LuckPerms data changes, not every tick. Every display is
 * tagged with a PDC marker so orphans (after a crash or /reload) can be swept.</p>
 */
public final class NameTagManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String HIDDEN_TEAM = "lnt_hidden";

    private final LemonNameTags plugin;
    private final LuckPerms luckPerms;
    private final RankGradients gradients;
    private final NamespacedKey markerKey;

    /** player uuid -> their display entity. */
    private final Map<UUID, TextDisplay> tags = new ConcurrentHashMap<>();

    public NameTagManager(LemonNameTags plugin, LuckPerms luckPerms, RankGradients gradients) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.gradients = gradients;
        this.markerKey = new NamespacedKey(plugin, "nametag");
    }

    // ── Text ──────────────────────────────────────────────────────────────────

    /** Builds "prefix + gradient-name" for the player from their LuckPerms data. */
    private Component buildText(Player player) {
        PlayerAdapter<Player> adapter = luckPerms.getPlayerAdapter(Player.class);
        CachedMetaData meta = adapter.getMetaData(player);
        String prefix = meta.getPrefix() != null ? meta.getPrefix() : "";

        User user = adapter.getUser(player);
        String group = user != null ? user.getPrimaryGroup() : null;
        // Use the visible display name (LemonCore sets it for /nick), so the
        // overhead tag shows the nick instead of exposing the real name.
        String visibleName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(player.displayName());
        if (visibleName.isBlank()) visibleName = player.getName();
        String name = gradients.forGroup(group).apply(visibleName);

        String full = prefix.isBlank() ? name : prefix + " " + name;

        // Optional second line with live hearts — enable on fight servers via
        // show-health; the listener refreshes the tag on damage/regen.
        if (plugin.getConfig().getBoolean("show-health", false)) {
            double hearts = Math.max(0, player.getHealth()) / 2.0;
            full += "<newline><red>❤ <white>"
                    + String.format(java.util.Locale.US, "%.1f", hearts);
        }
        return MM.deserialize(full);
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    /** (Re)creates the player's nametag display and mounts it. */
    public void create(Player player) {
        if (player == null) return;
        remove(player.getUniqueId()); // never leave a duplicate behind
        if (!player.isOnline()) return;

        final Component text = buildText(player);
        final double yOffset = plugin.getConfig().getDouble("display.y-offset", 0.4);
        final float viewRange = (float) plugin.getConfig().getDouble("display.view-range", 1.0);
        final boolean seeThrough = plugin.getConfig().getBoolean("display.see-through", false);
        final boolean shadow = plugin.getConfig().getBoolean("display.shadow", true);

        TextDisplay disp = player.getWorld().spawn(player.getLocation(), TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);        // always face the viewer
            d.setSeeThrough(seeThrough);
            d.setShadowed(shadow);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // transparent background
            d.setViewRange(viewRange);
            d.setPersistent(false);                           // never written to disk
            d.setTransformation(new Transformation(
                    new Vector3f(0f, (float) yOffset, 0f),
                    new Quaternionf(), new Vector3f(1f, 1f, 1f), new Quaternionf()));
            d.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        });

        player.addPassenger(disp);
        player.hideEntity(plugin, disp); // the wearer doesn't see their own tag
        tags.put(player.getUniqueId(), disp);
    }

    /** Refreshes the text only (used on LuckPerms changes); recreates if missing. */
    public void update(Player player) {
        if (player == null || !player.isOnline()) return;
        TextDisplay disp = tags.get(player.getUniqueId());
        if (disp == null || !disp.isValid() || !disp.getWorld().equals(player.getWorld())) {
            create(player);
            return;
        }
        disp.text(buildText(player));
        if (!player.getPassengers().contains(disp)) player.addPassenger(disp);
    }

    /** Removes and despawns a player's tag. */
    public void remove(UUID uuid) {
        TextDisplay disp = tags.remove(uuid);
        if (disp != null && disp.isValid()) disp.remove();
    }

    /**
     * Cheap periodic safety net: re-mounts/recreates tags that detached
     * (respawn, cross-world teleport) or vanished. Sends nothing unless a fix is
     * actually needed.
     */
    public void validateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            TextDisplay disp = tags.get(p.getUniqueId());
            if (disp == null || !disp.isValid() || !disp.getWorld().equals(p.getWorld())) {
                create(p);
                continue;
            }
            if (!p.getPassengers().contains(disp)) {
                p.addPassenger(disp);
            }
            // Refresh the text when it changed (nick set/cleared, prefix change) —
            // equal components send nothing, so the common case stays free.
            Component fresh = buildText(p);
            if (!fresh.equals(disp.text())) {
                disp.text(fresh);
            }
        }
    }

    /** Despawns every tracked tag (called on disable). */
    public void removeAll() {
        for (TextDisplay disp : tags.values()) {
            if (disp != null && disp.isValid()) disp.remove();
        }
        tags.clear();
    }

    /**
     * Sweeps every loaded world for our marked Text Displays and removes them.
     * Run on enable so a crash or /reload can't leave orphaned tags behind.
     */
    public int cleanupOrphans() {
        int removed = 0;
        for (World w : Bukkit.getWorlds()) {
            for (TextDisplay d : w.getEntitiesByClass(TextDisplay.class)) {
                if (d.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) {
                    d.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    // ── Vanilla nametag hiding ──────────────────────────────────────────────────

    /** Adds the player to a NEVER-nametag-visibility team so only our tag shows. */
    public void hideVanillaName(Player player) {
        if (!plugin.getConfig().getBoolean("hide-vanilla-nametags", true)) return;
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        // An entry can belong to only ONE team per scoreboard — never steal it
        // from another plugin's/datapack's team.
        Team existing = sb.getEntryTeam(player.getName());
        if (existing != null && !HIDDEN_TEAM.equals(existing.getName())) {
            return;
        }

        Team team = sb.getTeam(HIDDEN_TEAM);
        if (team == null) {
            team = sb.registerNewTeam(HIDDEN_TEAM);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
    }

    /** Removes the player from the hidden-nametag team (on quit). */
    public void showVanillaName(Player player) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = sb.getTeam(HIDDEN_TEAM);
        if (team != null && team.hasEntry(player.getName())) team.removeEntry(player.getName());
    }

    /**
     * Unregisters the hidden-name team entirely (all entries included). Called
     * on enable (crash cleanup — main-scoreboard teams persist in
     * scoreboard.dat, so entries from a crashed session would otherwise hide
     * vanilla names forever) and on disable, so no state outlives the plugin.
     */
    public void removeHiddenTeam() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = sb.getTeam(HIDDEN_TEAM);
        if (team != null) team.unregister();
    }
}

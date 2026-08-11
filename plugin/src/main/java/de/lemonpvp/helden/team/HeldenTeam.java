package de.lemonpvp.helden.team;

import de.lemonpvp.helden.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;

/** Eine Fraktion aus teams.yml. */
public final class HeldenTeam {

    private final String id;
    private final String display;
    private final String tag;
    private final ChatColor color;
    private final Location spawn;

    public HeldenTeam(String id, String display, String tag, ChatColor color, Location spawn) {
        this.id = id;
        this.display = display;
        this.tag = tag;
        this.color = color;
        this.spawn = spawn;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public String coloredDisplay() {
        return Text.color(display);
    }

    public String tag() {
        return tag;
    }

    public String coloredTag() {
        return Text.color(tag);
    }

    public ChatColor color() {
        return color;
    }

    public Location spawn() {
        return spawn == null ? null : spawn.clone();
    }

    public boolean hasSpawn() {
        return spawn != null;
    }
}

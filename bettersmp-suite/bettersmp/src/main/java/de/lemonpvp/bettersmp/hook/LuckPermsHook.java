package de.lemonpvp.bettersmp.hook;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Liest Prefix/Suffix/Primaergruppe aus LuckPerms.
 * Die LuckPerms-Klassen werden nur beruehrt, wenn das Plugin geladen ist
 * (innere Bridge-Klasse verhindert fruehes Classloading).
 */
public final class LuckPermsHook {

    private final boolean available;

    public LuckPermsHook() {
        this.available = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    public boolean isAvailable() {
        return available;
    }

    public String prefix(Player player) {
        return available ? Bridge.prefix(player) : "";
    }

    public String suffix(Player player) {
        return available ? Bridge.suffix(player) : "";
    }

    public String primaryGroup(Player player) {
        return available ? Bridge.group(player) : "default";
    }

    private static final class Bridge {

        private static CachedMetaData meta(Player player) {
            User user = LuckPermsProvider.get().getPlayerAdapter(Player.class).getUser(player);
            return user.getCachedData().getMetaData();
        }

        static String prefix(Player player) {
            try {
                String prefix = meta(player).getPrefix();
                return prefix == null ? "" : prefix;
            } catch (Exception e) {
                return "";
            }
        }

        static String suffix(Player player) {
            try {
                String suffix = meta(player).getSuffix();
                return suffix == null ? "" : suffix;
            } catch (Exception e) {
                return "";
            }
        }

        static String group(Player player) {
            try {
                return LuckPermsProvider.get().getPlayerAdapter(Player.class)
                        .getUser(player).getPrimaryGroup();
            } catch (Exception e) {
                return "default";
            }
        }
    }
}

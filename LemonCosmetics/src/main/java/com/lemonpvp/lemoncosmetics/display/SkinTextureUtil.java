package com.lemonpvp.lemoncosmetics.display;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Builds a {@code player_head} {@link ItemStack} carrying an arbitrary skin
 * texture — the trick that lets us show detailed 3D "hats" on a display entity
 * <b>without any resource pack</b> (task 3 of the cosmetic spec).
 *
 * <p>Mojang stores a skin as a signed "textures" property on a {@code GameProfile}:
 * a Base64 blob that decodes to a small JSON pointing at a {@code textures.minecraft.net}
 * URL. When a skull item carries such a profile, the vanilla client downloads and
 * renders that skin on the head model — all client-side, all vanilla. We attach
 * the profile via Paper's {@link PlayerProfile} API (no NMS/authlib reflection).</p>
 */
public final class SkinTextureUtil {

    private SkinTextureUtil() {}

    /**
     * @param base64Texture the Base64 value of the profile's {@code textures}
     *                      property (i.e. the encoded {@code {"textures":{"SKIN":{"url":...}}}} blob)
     * @return a player-head item textured with that skin
     */
    public static ItemStack head(String base64Texture) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (!(item.getItemMeta() instanceof SkullMeta meta)) return item;

        // A random UUID is fine: the client only reads the "textures" property, not
        // the identity. A stable UUID would let the client cache the skin — use a
        // deterministic one per cosmetic if you want cross-player texture caching.
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
        profile.setProperty(new ProfileProperty("textures", base64Texture));
        meta.setPlayerProfile(profile);

        item.setItemMeta(meta);
        return item;
    }
}

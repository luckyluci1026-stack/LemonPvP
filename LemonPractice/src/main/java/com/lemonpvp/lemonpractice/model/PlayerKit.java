package com.lemonpvp.lemonpractice.model;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class PlayerKit {
    private final java.util.UUID playerUuid;
    private final String gamemode;
    private final Map<Integer, ItemStack> slots = new HashMap<>();

    public PlayerKit(java.util.UUID playerUuid, String gamemode) {
        this.playerUuid = playerUuid;
        this.gamemode = gamemode;
    }

    public java.util.UUID getPlayerUuid() { return playerUuid; }
    public String getGamemode() { return gamemode; }
    public Map<Integer, ItemStack> getSlots() { return slots; }
    public void setSlot(int slot, ItemStack item) { slots.put(slot, item); }
    public ItemStack getSlot(int slot) { return slots.get(slot); }
    public void clearSlots() { slots.clear(); }
}

package com.lemonpvp.lemoncosmetics.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerCosmetics {

    private final UUID playerUuid;
    private final Set<String> ownedEffects;
    private String activeEffectId;
    private final Set<String> ownedPatterns;
    private final Set<String> ownedMaterials;
    /**
     * Maps armor slot key ("head", "chest", "legs", "feet") to a two-element
     * array where [0] = patternId and [1] = materialId.
     */
    private final Map<String, String[]> appliedTrims;

    public PlayerCosmetics(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.ownedEffects = new HashSet<>();
        this.activeEffectId = null;
        this.ownedPatterns = new HashSet<>();
        this.ownedMaterials = new HashSet<>();
        this.appliedTrims = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Set<String> getOwnedEffects() {
        return ownedEffects;
    }

    public String getActiveEffectId() {
        return activeEffectId;
    }

    public void setActiveEffectId(String activeEffectId) {
        this.activeEffectId = activeEffectId;
    }

    public Set<String> getOwnedPatterns() {
        return ownedPatterns;
    }

    public Set<String> getOwnedMaterials() {
        return ownedMaterials;
    }

    public Map<String, String[]> getAppliedTrims() {
        return appliedTrims;
    }

    // -------------------------------------------------------------------------
    // Ownership helpers
    // -------------------------------------------------------------------------

    public boolean ownsEffect(String id) {
        return id != null && ownedEffects.contains(id);
    }

    public boolean ownsPattern(String id) {
        return id != null && ownedPatterns.contains(id);
    }

    public boolean ownsMaterial(String id) {
        return id != null && ownedMaterials.contains(id);
    }

    // -------------------------------------------------------------------------
    // Trim helpers
    // -------------------------------------------------------------------------

    /**
     * Stores a trim for the given slot. If either {@code patternId} or
     * {@code materialId} is {@code null} the trim entry is removed instead.
     *
     * @param slot       one of "head", "chest", "legs", "feet"
     * @param patternId  trim pattern id, or {@code null} to remove
     * @param materialId trim material id, or {@code null} to remove
     */
    public void setAppliedTrim(String slot, String patternId, String materialId) {
        if (patternId == null || materialId == null) {
            appliedTrims.remove(slot);
        } else {
            appliedTrims.put(slot, new String[]{patternId, materialId});
        }
    }

    /**
     * Returns the trim stored for {@code slot} as {@code [patternId, materialId]},
     * or {@code null} if no trim is applied to that slot.
     *
     * @param slot one of "head", "chest", "legs", "feet"
     * @return two-element array or {@code null}
     */
    public String[] getAppliedTrim(String slot) {
        return appliedTrims.get(slot);
    }
}

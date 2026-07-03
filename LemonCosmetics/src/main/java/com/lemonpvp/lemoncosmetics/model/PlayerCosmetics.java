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
    private String activeDeathEffectId = null;
    private final Set<String> ownedDeathEffects = new HashSet<>();
    private String activeWinEffectId = null;
    private final Set<String> ownedWinEffects = new HashSet<>();
    private String activeTrailId = null;
    private final Set<String> ownedTrails = new HashSet<>();
    private String equippedTagId = null;
    private final Set<String> ownedTags = new HashSet<>();

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
    // Death effect getters / setters
    // -------------------------------------------------------------------------

    public String getActiveDeathEffectId() {
        return activeDeathEffectId;
    }

    public void setActiveDeathEffectId(String activeDeathEffectId) {
        this.activeDeathEffectId = activeDeathEffectId;
    }

    public Set<String> getOwnedDeathEffects() {
        return ownedDeathEffects;
    }

    public boolean ownsDeathEffect(String id) {
        return id != null && ownedDeathEffects.contains(id);
    }

    // -------------------------------------------------------------------------
    // Win effect getters / setters
    // -------------------------------------------------------------------------

    public String getActiveWinEffectId() {
        return activeWinEffectId;
    }

    public void setActiveWinEffectId(String activeWinEffectId) {
        this.activeWinEffectId = activeWinEffectId;
    }

    public Set<String> getOwnedWinEffects() {
        return ownedWinEffects;
    }

    public boolean ownsWinEffect(String id) {
        return id != null && ownedWinEffects.contains(id);
    }

    // -------------------------------------------------------------------------
    // Arrow trail getters / setters
    // -------------------------------------------------------------------------

    public String getActiveTrailId() {
        return activeTrailId;
    }

    public void setActiveTrailId(String activeTrailId) {
        this.activeTrailId = activeTrailId;
    }

    public Set<String> getOwnedTrails() {
        return ownedTrails;
    }

    public boolean ownsTrail(String id) {
        return id != null && ownedTrails.contains(id);
    }

    // -------------------------------------------------------------------------
    // Tag getters / setters
    // -------------------------------------------------------------------------

    public String getEquippedTagId() {
        return equippedTagId;
    }

    public void setEquippedTagId(String equippedTagId) {
        this.equippedTagId = equippedTagId;
    }

    public Set<String> getOwnedTags() {
        return ownedTags;
    }

    public boolean ownsTag(String id) {
        return id != null && ownedTags.contains(id);
    }

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

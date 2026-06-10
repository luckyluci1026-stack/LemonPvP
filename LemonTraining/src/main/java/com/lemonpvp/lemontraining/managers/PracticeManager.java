package com.lemonpvp.lemontraining.managers;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.lemonpvp.lemontraining.model.PracticeSession;
import com.lemonpvp.lemontraining.practice.*;
import com.lemonpvp.lemontraining.practice.BowPractice;
import com.lemonpvp.lemontraining.practice.CrystalPractice;
import com.lemonpvp.lemontraining.practice.MacePractice;
import com.lemonpvp.lemontraining.practice.SwordPractice;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PracticeManager {

    private final LemonTraining plugin;
    private final Map<UUID, AbstractPractice> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, PracticeMode> pendingModes = new ConcurrentHashMap<>();

    public PracticeManager(LemonTraining plugin) {
        this.plugin = plugin;
    }

    public void startPractice(Player player, PracticeMode mode) {
        // End any existing session first (without reconnecting)
        endPractice(player.getUniqueId(), false);

        PracticeSession session = new PracticeSession(player.getUniqueId(), mode, System.currentTimeMillis());

        AbstractPractice practice = switch (mode) {
            case TOTEM   -> new TotemPractice(plugin, player, session);
            case BOW     -> new BowPractice(plugin, player, session);
            case MACE    -> new MacePractice(plugin, player, session);
            case SWORD   -> new SwordPractice(plugin, player, session);
            case CRYSTAL -> new CrystalPractice(plugin, player, session);
        };

        activeSessions.put(player.getUniqueId(), practice);
        plugin.getDatabase().startSessionAsync(player.getUniqueId(), mode);
        practice.start();
    }

    public void endPractice(UUID uuid, boolean sendToLobby) {
        AbstractPractice practice = activeSessions.remove(uuid);
        if (practice == null) return;

        plugin.getDatabase().endSessionAsync(uuid, practice.getSession().getHits());

        if (sendToLobby) {
            practice.end();
        } else {
            practice.cancelTasks();
            // Clean up spawned entities without sending player to lobby
            if (practice instanceof MacePractice mp) {
                for (UUID zombieId : mp.getSpawnedZombies()) {
                    org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(zombieId);
                    if (e != null) e.remove();
                }
            } else if (practice instanceof SwordPractice sp) {
                if (sp.getZombieUUID() != null) {
                    org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(sp.getZombieUUID());
                    if (e != null) e.remove();
                }
            } else if (practice instanceof CrystalPractice cp) {
                if (cp.getZombieUUID() != null) {
                    org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(cp.getZombieUUID());
                    if (e != null) e.remove();
                }
            } else if (practice instanceof BowPractice bp) {
                bp.cleanup();
            } else if (practice instanceof TotemPractice tp) {
                tp.cleanup();
            }
        }
    }

    public void schedulePending(UUID uuid, PracticeMode mode) {
        pendingModes.put(uuid, mode);
    }

    public boolean hasPending(UUID uuid) {
        return pendingModes.containsKey(uuid);
    }

    public PracticeMode pollPending(UUID uuid) {
        return pendingModes.remove(uuid);
    }

    public PracticeMode consumePending(UUID uuid) {
        return pendingModes.remove(uuid);
    }

    public Map<UUID, AbstractPractice> activeSessions() {
        return activeSessions;
    }

    public AbstractPractice getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public boolean isInSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public void endAllSessions() {
        for (UUID uuid : activeSessions.keySet()) {
            endPractice(uuid, false);
        }
    }
}

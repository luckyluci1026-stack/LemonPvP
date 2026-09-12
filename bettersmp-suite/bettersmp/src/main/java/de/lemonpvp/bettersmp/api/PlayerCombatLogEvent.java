package de.lemonpvp.bettersmp.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Wird gefeuert, wenn ein Spieler während eines Kampfes ausloggt -
 * BEVOR BetterSMP die Bestrafung (Kill) ausführt.
 * Andere Plugins (z.B. Lifesteal+) können hier z.B. Herzverlust
 * und Kill-Gutschrift für den Gegner umsetzen.
 */
public class PlayerCombatLogEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID opponent;

    public PlayerCombatLogEvent(@NotNull Player player, @Nullable UUID opponent) {
        this.player = player;
        this.opponent = opponent;
    }

    /** Der Spieler, der im Kampf ausgeloggt hat (noch online während des Events). */
    public @NotNull Player getPlayer() {
        return player;
    }

    /** UUID des letzten Kampf-Gegners, falls bekannt. */
    public @Nullable UUID getOpponent() {
        return opponent;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

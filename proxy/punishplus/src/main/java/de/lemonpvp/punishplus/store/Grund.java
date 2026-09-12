package de.lemonpvp.punishplus.store;

/** Ein vorgefertigter Grund aus bans.yml. offendDauerMillis gilt nur fuer /offend. */
public record Grund(String id, String text, long offendDauerMillis) {
}

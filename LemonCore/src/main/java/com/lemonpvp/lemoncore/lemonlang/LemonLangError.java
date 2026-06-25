package com.lemonpvp.lemoncore.lemonlang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Fehler beim Laden oder Ausfuehren eines LemonLang-Skripts.
 *
 * <p>Traegt die Skriptdatei, die Zeilennummer, eine Nachricht und optional einen
 * Hinweis ("hint") wie man den Fehler behebt. Bietet Formatierung fuer Konsole
 * (Klartext) und Chat (MiniMessage, rot).
 */
public class LemonLangError extends RuntimeException {

    private final String scriptFile;
    private final int line;
    private final String hint;

    public LemonLangError(String scriptFile, int line, String message) {
        this(scriptFile, line, message, null);
    }

    public LemonLangError(String scriptFile, int line, String message, String hint) {
        super(message);
        this.scriptFile = scriptFile;
        this.line = line;
        this.hint = hint;
    }

    public String scriptFile() {
        return scriptFile;
    }

    public int line() {
        return line;
    }

    public String hint() {
        return hint;
    }

    /**
     * Klartext-Darstellung fuer die Server-Konsole.
     */
    public String formatForConsole() {
        StringBuilder sb = new StringBuilder();
        sb.append("[LemonLang] Fehler in ")
          .append(scriptFile == null ? "?" : scriptFile)
          .append(":")
          .append(line)
          .append(" - ")
          .append(getMessage() == null ? "" : getMessage());
        if (hint != null && !hint.isEmpty()) {
            sb.append("\n           Tipp: ").append(hint);
        }
        return sb.toString();
    }

    /**
     * MiniMessage-Darstellung (rot) fuer den Spieler-Chat.
     */
    public Component formatForChat() {
        StringBuilder raw = new StringBuilder();
        raw.append("<red>[LemonLang] Fehler in <yellow>")
           .append(escape(scriptFile == null ? "?" : scriptFile))
           .append("<red>:<yellow>")
           .append(line)
           .append("<red> - ")
           .append(escape(getMessage() == null ? "" : getMessage()));
        if (hint != null && !hint.isEmpty()) {
            raw.append("<newline><gray>Tipp: ").append(escape(hint));
        }
        return MiniMessage.miniMessage().deserialize(raw.toString());
    }

    /**
     * Maskiert MiniMessage-Sonderzeichen in dynamischem Text, damit Nutzerinhalt
     * nicht als Tag interpretiert wird.
     */
    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("<", "\\<");
    }

    @Override
    public String toString() {
        return formatForConsole();
    }
}
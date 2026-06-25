package com.lemonpvp.lemoncore.lemonlang.token;

/**
 * Eine bedeutsame Zeile eines LemonLang-Skripts.
 *
 * @param indent  Anzahl der fuehrenden Leerzeichen (Einrueckung)
 * @param content getrimmter Inhalt der Zeile (ohne fuehrende/abschliessende Leerzeichen)
 * @param number  Zeilennummer in der Originaldatei (1-basiert)
 */
public record Line(int indent, String content, int number) {
}
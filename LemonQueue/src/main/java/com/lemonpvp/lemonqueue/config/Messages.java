package com.lemonpvp.lemonqueue.config;

import com.lemonpvp.lemonqueue.util.Gradients;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All user-facing text for LemonQueue, with two built-in languages
 * ({@code de} / {@code en}) and optional per-message overrides.
 *
 * <p>Each message has a key. If the {@code messages:} section in config.yml
 * supplies a non-blank value for that key, it is used verbatim as a MiniMessage
 * template (the server owner controls colours, gradients and bold). Otherwise
 * the built-in, language-dependent default is used — and for the live display
 * messages (action bar, title, subtitle) that default is the flowing,
 * <b>bold</b> animated gradient.</p>
 *
 * <p>Placeholders available in overrides: {@code {pos} {total} {eta} {target}
 * {server} {size} {max} {count}}.</p>
 */
public final class Messages {

    public enum Lang {
        DE, EN;
        static Lang from(String s) { return s != null && s.trim().equalsIgnoreCase("en") ? EN : DE; }
    }

    private final Lang lang;
    private final Map<String, String> overrides;

    private Messages(Lang lang, Map<String, String> overrides) {
        this.lang = lang;
        this.overrides = overrides;
    }

    @SuppressWarnings("unchecked")
    public static Messages load(String language, Object messagesSection) {
        Map<String, String> ov = new LinkedHashMap<>();
        if (messagesSection instanceof Map<?, ?> raw) {
            for (Map.Entry<?, ?> e : ((Map<String, Object>) raw).entrySet()) {
                if (e.getValue() != null) ov.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        return new Messages(Lang.from(language), ov);
    }

    // English-only: the network displays English regardless of the configured language.
    private boolean en() { return true; }

    /** Returns a non-blank override template for {@code key}, or {@code null}. */
    private String override(String key) {
        String v = overrides.get(key);
        return (v != null && !v.isBlank()) ? v : null;
    }

    // ── Live display (animated + bold by default) ─────────────────────────

    public Component actionbarWaiting(int pos, int total, String eta) {
        String o = override("actionbar-waiting");
        if (o != null) return Gradients.template(o, ph("pos", "" + pos, "total", "" + total, "eta", eta));
        String label = en() ? "⏳ In queue " : "⏳ Warteschlange ";
        String posL  = en() ? "Position " + pos + " / " + total : "Platz " + pos + " / " + total;
        String etaL  = en() ? "approx. " + eta : "ca. " + eta;
        return Component.text()
                .append(Gradients.fireAnimated(label))
                .append(Component.text("» ", NamedTextColor.DARK_GRAY))
                .append(Gradients.lemonAnimated(posL))
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Gradients.coolAnimated(etaL))
                .build()
                .decorate(TextDecoration.BOLD);
    }

    public Component actionbarConnected(String target) {
        String o = override("actionbar-connected");
        if (o != null) return Gradients.template(o, ph("target", target));
        String txt = en() ? "✔ Connected to " + target + "!" : "✔ Verbunden mit " + target + "!";
        return Gradients.rainbowAnimated(txt).decorate(TextDecoration.BOLD);
    }

    public Component title(int pos, int total, String eta) {
        String o = override("title");
        if (o != null) return Gradients.template(o, ph("pos", "" + pos, "total", "" + total, "eta", eta));
        String txt = en() ? "In queue" : "In der Warteschlange";
        return Gradients.lemonAnimated(txt).decorate(TextDecoration.BOLD);
    }

    public Component subtitle(int pos, int total, String eta) {
        String o = override("subtitle");
        if (o != null) return Gradients.template(o, ph("pos", "" + pos, "total", "" + total, "eta", eta));
        String posL = en() ? "Position " + pos + " / " + total : "Platz " + pos + " / " + total;
        String etaL = en() ? "approx. " + eta : "ca. " + eta;
        return Component.text()
                .append(Gradients.fireAnimated(posL))
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Gradients.coolAnimated(etaL))
                .build()
                .decorate(TextDecoration.BOLD);
    }

    // ── One-shot messages ─────────────────────────────────────────────────

    public Component joinTitle() {
        String o = override("join-title");
        if (o != null) return Gradients.template(o, ph());
        return Gradients.lemon(en() ? "In queue" : "In der Warteschlange").decorate(TextDecoration.BOLD);
    }

    public Component joinSubtitle(int pos, int total) {
        String o = override("join-subtitle");
        if (o != null) return Gradients.template(o, ph("pos", "" + pos, "total", "" + total));
        String txt = en() ? "Position " + pos + " of " + total : "Platz " + pos + " von " + total;
        return Gradients.fire(txt).decorate(TextDecoration.BOLD);
    }

    public Component joinMessage(int pos, int total) {
        String o = override("join-message");
        if (o != null) return Gradients.template(o, ph("pos", "" + pos, "total", "" + total));
        String txt = en()
                ? "» The server is full – you've been added to the queue (position " + pos + "/" + total + ")."
                : "» Der Server ist voll – du wurdest in die Warteschlange aufgenommen (Platz " + pos + "/" + total + ").";
        return Gradients.lemonAnimated(txt);
    }

    public Component kickFull(String server) {
        String o = override("kick-full");
        if (o != null) return Gradients.template(o, ph("server", server));
        String txt = en()
                ? server + " is full – you're now waiting in the queue."
                : "Der Server " + server + " ist voll – du wartest jetzt in der Warteschlange.";
        return Gradients.fire(txt);
    }

    // ── Command feedback ──────────────────────────────────────────────────

    public Component cmdPosition(int pos) {
        String o = override("cmd-position");
        if (o != null) return Gradients.template(o, ph("pos", "" + pos));
        return Gradients.lemonAnimated(en()
                ? "You're in the queue – position " + pos + "."
                : "Du stehst in der Warteschlange – Platz " + pos + ".");
    }

    public Component cmdNotQueued() {
        String o = override("cmd-not-queued");
        if (o != null) return Gradients.template(o, ph());
        return Gradients.lemon(en() ? "You're not in any queue." : "Du stehst aktuell in keiner Warteschlange.");
    }

    public Component cmdLeft() {
        String o = override("cmd-left");
        if (o != null) return Gradients.template(o, ph());
        return Gradients.fire(en() ? "You left the queue." : "Du hast die Warteschlange verlassen.");
    }

    /** Disconnect screen shown when a player leaves the queue (kicked from proxy). */
    public Component leaveProxy() {
        String o = override("leave-proxy");
        if (o != null) return Gradients.template(o, ph());
        return Gradients.lemon(en()
                ? "You left the queue. Reconnect to rejoin."
                : "Du hast die Warteschlange verlassen. Verbinde dich erneut.");
    }

    public Component cmdNotInQueue() {
        String o = override("cmd-not-in-queue");
        if (o != null) return Gradients.template(o, ph());
        return Component.text(en() ? "You're not in a queue." : "Du stehst in keiner Warteschlange.", NamedTextColor.GRAY);
    }

    public Component cmdPlayersOnly() {
        String o = override("cmd-players-only");
        if (o != null) return Gradients.template(o, ph());
        return Component.text(en() ? "Only players can leave the queue." : "Nur Spieler können die Warteschlange verlassen.",
                NamedTextColor.RED);
    }

    public Component cmdNoPerm() {
        String o = override("cmd-no-perm");
        if (o != null) return Gradients.template(o, ph());
        return Component.text(en() ? "You don't have permission for that." : "Dazu fehlt dir die Berechtigung.",
                NamedTextColor.RED);
    }

    public Component cmdClearUsage() {
        String o = override("cmd-clear-usage");
        if (o != null) return Gradients.template(o, ph());
        return Component.text(en() ? "Usage: /lq clear <server>" : "Nutzung: /lq clear <server>", NamedTextColor.RED);
    }

    public Component cmdCleared(String server) {
        String o = override("cmd-cleared");
        if (o != null) return Gradients.template(o, ph("server", server));
        return Gradients.lemon(en() ? "Cleared the queue for '" + server + "'." : "Warteschlange für '" + server + "' geleert.");
    }

    public Component cmdReloaded() {
        String o = override("cmd-reloaded");
        if (o != null) return Gradients.template(o, ph());
        return Gradients.lemon(en() ? "✔ Config reloaded." : "✔ Config neu geladen.");
    }

    public Component cmdUnknown() {
        String o = override("cmd-unknown");
        if (o != null) return Gradients.template(o, ph());
        return Component.text(en()
                ? "Unknown. Usage: /lq [leave|admin|reload|clear <server>]"
                : "Unbekannt. Nutzung: /lq [leave|admin|reload|clear <server>]", NamedTextColor.RED);
    }

    public Component adminHeader() {
        String o = override("admin-header");
        if (o != null) return Gradients.template(o, ph());
        return Gradients.rainbowAnimated("══════ LemonQueue ══════");
    }

    public Component adminNone() {
        String o = override("admin-none");
        if (o != null) return Gradients.template(o, ph());
        return Component.text(en() ? "No active queues." : "Keine aktiven Warteschlangen.", NamedTextColor.GRAY);
    }

    public Component adminLine(String server, int size, String max) {
        String o = override("admin-line");
        if (o != null) return Gradients.template(o, ph("server", server, "size", "" + size, "max", max));
        return Component.text()
                .append(Gradients.lemon(server))
                .append(Component.text(en()
                        ? " – " + size + " waiting (max " + max + ")"
                        : " – " + size + " wartend (max " + max + ")", NamedTextColor.GRAY))
                .build();
    }

    public Component adminTotal(int count) {
        String o = override("admin-total");
        if (o != null) return Gradients.template(o, ph("count", "" + count, "total", "" + count));
        return Component.text(en() ? "Total: " + count + " players" : "Gesamt: " + count + " Spieler",
                NamedTextColor.DARK_GRAY);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private static Map<String, String> ph(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }
}

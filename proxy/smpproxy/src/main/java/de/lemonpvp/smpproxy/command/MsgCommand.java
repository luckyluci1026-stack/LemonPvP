package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class MsgCommand implements SimpleCommand {

    private static final int MAX_LAENGE = 256;

    private final SMPProxy plugin;
    private final boolean antwort;

    public MsgCommand(SMPProxy plugin, boolean antwort) {
        this.plugin = plugin;
        this.antwort = antwort;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(plugin.message("player-only"));
            return;
        }
        String[] args = invocation.arguments();
        if (antwort) {
            if (args.length == 0) {
                sender.sendMessage(plugin.message("reply-usage"));
                return;
            }
            plugin.privatnachrichten().antworten(sender, text(args, 0));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.message("msg-usage"));
            return;
        }
        plugin.privatnachrichten().schreiben(sender, args[0], text(args, 1));
    }

    private static String text(String[] args, int ab) {
        String text = String.join(" ", Arrays.copyOfRange(args, ab, args.length)).trim();
        return text.length() > MAX_LAENGE ? text.substring(0, MAX_LAENGE) : text;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (antwort || args.length > 1) {
            return List.of();
        }
        String anfang = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> namen = new ArrayList<>();
        for (Player spieler : plugin.proxy().getAllPlayers()) {
            if (spieler.getUsername().toLowerCase(Locale.ROOT).startsWith(anfang)) {
                namen.add(spieler.getUsername());
            }
        }
        return namen;
    }
}

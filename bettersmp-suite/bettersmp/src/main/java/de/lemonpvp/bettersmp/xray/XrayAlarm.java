package de.lemonpvp.bettersmp.xray;

import de.lemonpvp.bettersmp.BetterSMP;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class XrayAlarm implements Listener {

    static final int MINUTEN = 61;
    static final int MAX_POSITIONEN = 1024;
    static final long ADER_MILLIS = 5 * 60_000L;
    private static final int ADER_ABSTAND_QUADRAT = 9;
    private static final int MAX_ERZE = 64;
    private static final int MAX_FUNDE = 500;
    private static final BlockFace[] SEITEN = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST};
    private static final Set<Material> STANDARD_ERZE = EnumSet.of(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS);
    private static final DateTimeFormatter PROTOKOLL_ZEIT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    record Fund(long zeit, Material art, boolean versteckt) {
    }

    static final class Verlauf {
        final UUID id;
        String name;
        final long beginn;
        final long[] minute = new long[MINUTEN];
        final int[] bloecke = new int[MINUTEN];
        final Deque<Fund> funde = new ArrayDeque<>();
        final Deque<long[]> erze = new ArrayDeque<>();
        final LinkedHashSet<Long> abgebaut = new LinkedHashSet<>();
        final Map<String, int[]> arten = new LinkedHashMap<>();
        UUID welt;
        long bloeckeGesamt;
        int versteckteGesamt;
        long letzterAlarm;

        Verlauf(UUID id, String name, long beginn) {
            this.id = id;
            this.name = name;
            this.beginn = beginn;
        }
    }

    private final BetterSMP plugin;
    private final Map<UUID, Verlauf> verlaeufe = new HashMap<>();

    public XrayAlarm(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimAbbauen(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("anti-xray.alarm.enabled", true)) {
            return;
        }
        Player spieler = event.getPlayer();
        if (spieler.getGameMode() == GameMode.CREATIVE || spieler.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Block block = event.getBlock();
        World.Environment umgebung = block.getWorld().getEnvironment();
        if (umgebung != World.Environment.NETHER && (umgebung != World.Environment.NORMAL || block.getY() >= 64)) {
            return;
        }
        List<Long> offen = new ArrayList<>(6);
        for (BlockFace seite : SEITEN) {
            Block nachbar = block.getRelative(seite);
            if (!nachbar.getType().isOccluding()) {
                offen.add(schluessel(nachbar.getX(), nachbar.getY(), nachbar.getZ()));
            }
        }
        abgebaut(spieler.getUniqueId(), spieler.getName(), block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(),
                block.getType(), offen, System.currentTimeMillis());
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        Verlauf verlauf = verlaeufe.get(event.getPlayer().getUniqueId());
        if (verlauf != null) {
            verlauf.abgebaut.clear();
            verlauf.erze.clear();
        }
    }

    void abgebaut(UUID id, String name, UUID welt, int x, int y, int z, Material art, Collection<Long> offen, long zeit) {
        Verlauf verlauf = verlaeufe.computeIfAbsent(id, neu -> new Verlauf(neu, name, zeit));
        verlauf.name = name;
        if (!welt.equals(verlauf.welt)) {
            verlauf.welt = welt;
            verlauf.abgebaut.clear();
            verlauf.erze.clear();
        }
        long schluessel = schluessel(x, y, z);
        if (erze().contains(art)) {
            while (!verlauf.erze.isEmpty() && zeit - verlauf.erze.peekFirst()[0] > ADER_MILLIS) {
                verlauf.erze.pollFirst();
            }
            boolean neueAder = true;
            for (long[] erz : verlauf.erze) {
                if (abstandQuadrat(erz[1], schluessel) <= ADER_ABSTAND_QUADRAT) {
                    neueAder = false;
                    break;
                }
            }
            verlauf.erze.addLast(new long[]{zeit, schluessel});
            if (verlauf.erze.size() > MAX_ERZE) {
                verlauf.erze.pollFirst();
            }
            if (neueAder) {
                boolean versteckt = verlauf.abgebaut.containsAll(offen);
                verlauf.funde.addLast(new Fund(zeit, art, versteckt));
                while (verlauf.funde.size() > MAX_FUNDE || zeit - verlauf.funde.peekFirst().zeit() > 60 * 60_000L) {
                    verlauf.funde.pollFirst();
                }
                int[] zaehler = verlauf.arten.computeIfAbsent(artName(art), neu -> new int[2]);
                zaehler[0]++;
                if (versteckt) {
                    zaehler[1]++;
                    verlauf.versteckteGesamt++;
                    pruefen(verlauf, zeit);
                }
            }
        } else {
            long minute = zeit / 60_000L;
            int platz = (int) (minute % MINUTEN);
            if (verlauf.minute[platz] != minute) {
                verlauf.minute[platz] = minute;
                verlauf.bloecke[platz] = 0;
            }
            verlauf.bloecke[platz]++;
            verlauf.bloeckeGesamt++;
        }
        verlauf.abgebaut.remove(schluessel);
        verlauf.abgebaut.add(schluessel);
        if (verlauf.abgebaut.size() > MAX_POSITIONEN) {
            Iterator<Long> aelteste = verlauf.abgebaut.iterator();
            aelteste.next();
            aelteste.remove();
        }
    }

    private void pruefen(Verlauf verlauf, long zeit) {
        FileConfiguration config = plugin.getConfig();
        int fenster = fenster();
        int minFunde = Math.max(2, config.getInt("anti-xray.alarm.min-funde", 4));
        int grenze = grenze();
        long pause = Math.max(1, config.getInt("anti-xray.alarm.pause-minuten", 10)) * 60_000L;
        Map<String, Integer> arten = new LinkedHashMap<>();
        int versteckte = versteckteSeit(verlauf, zeit - fenster * 60_000L, arten);
        int bloecke = bloeckeImFenster(verlauf, zeit, fenster);
        if (versteckte < minFunde || bloecke > versteckte * grenze) {
            return;
        }
        if (verlauf.letzterAlarm != 0 && zeit - verlauf.letzterAlarm < pause) {
            return;
        }
        verlauf.letzterAlarm = zeit;
        alarmieren(verlauf, fenster, versteckte, bloecke / versteckte, arten);
    }

    private void alarmieren(Verlauf verlauf, int fenster, int funde, int schnitt, Map<String, Integer> arten) {
        List<String> teile = new ArrayList<>();
        arten.forEach((art, anzahl) -> teile.add(anzahl + "x " + art));
        String artenText = String.join(", ", teile);
        Component text = plugin.msgs().format("xray.alarm", "player", verlauf.name, "minuten", String.valueOf(fenster),
                "funde", String.valueOf(funde), "arten", artenText, "schnitt", String.valueOf(schnitt));
        for (Player team : Bukkit.getOnlinePlayers()) {
            if (team.hasPermission("bettersmp.xray.alarm")) {
                team.sendMessage(text);
            }
        }
        String zeile = verlauf.name + ": " + funde + " versteckte Erze freigelegt (" + artenText + ") in " + fenster
                + " Min, " + schnitt + " Blöcke pro Fund";
        plugin.getLogger().warning("[X-Ray?] " + zeile);
        File protokoll = new File(plugin.getDataFolder(), "xray-alarme.log");
        String eintrag = PROTOKOLL_ZEIT.format(LocalDateTime.now()) + "  " + zeile + System.lineSeparator();
        Runnable schreiben = () -> {
            try {
                Files.createDirectories(protokoll.getAbsoluteFile().getParentFile().toPath());
                Files.writeString(protokoll.toPath(), eintrag, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException fehler) {
                plugin.getLogger().warning("xray-alarme.log konnte nicht geschrieben werden: " + fehler.getMessage());
            }
        };
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, schreiben);
        } else {
            schreiben.run();
        }
    }

    int versteckteSeit(Verlauf verlauf, long seit, Map<String, Integer> arten) {
        int anzahl = 0;
        for (Fund fund : verlauf.funde) {
            if (fund.zeit() >= seit && fund.versteckt()) {
                anzahl++;
                arten.merge(artName(fund.art()), 1, Integer::sum);
            }
        }
        return anzahl;
    }

    static int bloeckeImFenster(Verlauf verlauf, long zeit, int fenster) {
        long jetzt = zeit / 60_000L;
        int summe = 0;
        for (int i = 0; i < fenster; i++) {
            long minute = jetzt - i;
            int platz = (int) (minute % MINUTEN);
            if (verlauf.minute[platz] == minute) {
                summe += verlauf.bloecke[platz];
            }
        }
        return summe;
    }

    int fenster() {
        return Math.max(1, Math.min(MINUTEN - 1, plugin.getConfig().getInt("anti-xray.alarm.fenster-minuten", 20)));
    }

    int grenze() {
        return Math.max(1, plugin.getConfig().getInt("anti-xray.alarm.max-bloecke-pro-fund", 40));
    }

    Set<Material> erze() {
        List<String> namen = plugin.getConfig().getStringList("anti-xray.alarm.erze");
        if (namen.isEmpty()) {
            return STANDARD_ERZE;
        }
        Set<Material> erze = EnumSet.noneOf(Material.class);
        for (String name : namen) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                erze.add(material);
            }
        }
        return erze.isEmpty() ? STANDARD_ERZE : erze;
    }

    static String artName(Material art) {
        return switch (art) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> "Diamant";
            case ANCIENT_DEBRIS -> "Antiker Schrott";
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> "Smaragd";
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> "Gold";
            default -> {
                String klein = art.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                yield Character.toUpperCase(klein.charAt(0)) + klein.substring(1);
            }
        };
    }

    static long schluessel(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
    }

    static int[] position(long schluessel) {
        int x = (int) (schluessel >> 38);
        int z = (int) (schluessel << 26 >> 38);
        int y = (int) (schluessel << 52 >> 52);
        return new int[]{x, y, z};
    }

    static long abstandQuadrat(long a, long b) {
        int[] p = position(a);
        int[] q = position(b);
        long dx = p[0] - q[0];
        long dy = p[1] - q[1];
        long dz = p[2] - q[2];
        return dx * dx + dy * dy + dz * dz;
    }

    Verlauf verlauf(UUID id) {
        return verlaeufe.get(id);
    }

    Verlauf verlauf(String name) {
        for (Verlauf verlauf : verlaeufe.values()) {
            if (verlauf.name.equalsIgnoreCase(name)) {
                return verlauf;
            }
        }
        return null;
    }

    Collection<Verlauf> alle() {
        return verlaeufe.values();
    }
}

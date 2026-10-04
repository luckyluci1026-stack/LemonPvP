package de.lemonpvp.duelplus.replay;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.arena.Arena;
import de.lemonpvp.duelplus.db.ReplayEintrag;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ReplayWiedergabe {

    public static final double[] TEMPI = {0.25, 0.5, 1, 2, 4};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final DuelPlus plugin;
    private final ReplayEintrag eintrag;
    private final ReplayDaten.Replay replay;
    private final Arena arena;
    private final UUID zuschauer;
    private final String herkunft;
    private final GameMode vorherModus;
    private final Location vorherOrt;
    private final Location mitte;
    private final Mannequin[] figuren;
    private final byte[][][] ausruestung;
    private final Map<Integer, ItemDisplay> entities = new HashMap<>();
    private final Map<Integer, String> entityMaterial = new HashMap<>();
    private final Map<Integer, float[]> entityZiel = new HashMap<>();
    private final Map<Long, BlockState> urspruenge = new LinkedHashMap<>();
    private int position = -1;
    private double tempo = 1;
    private double vorrat;
    private boolean pausiert;
    private boolean endeGezeigt;
    private int anzeigeTakt;
    private BukkitTask takt;
    private boolean beendet;

    public ReplayWiedergabe(DuelPlus plugin, ReplayEintrag eintrag, ReplayDaten.Replay replay, Arena arena,
                            Player zuschauer, String herkunft) {
        this.plugin = plugin;
        this.eintrag = eintrag;
        this.replay = replay;
        this.arena = arena;
        this.zuschauer = zuschauer.getUniqueId();
        this.herkunft = herkunft;
        this.vorherModus = zuschauer.getGameMode();
        this.vorherOrt = zuschauer.getLocation();
        this.mitte = arena.world().getSpawnLocation().clone();
        this.figuren = new Mannequin[replay.kopf().spieler().size()];
        this.ausruestung = new byte[figuren.length][ReplayDaten.SLOTS][];
    }

    public ReplayEintrag eintrag() {
        return eintrag;
    }

    public Arena arena() {
        return arena;
    }

    public String herkunft() {
        return herkunft;
    }

    public int bilder() {
        return replay.bilder().size();
    }

    public int position() {
        return Math.max(0, position);
    }

    public double tempo() {
        return tempo;
    }

    public boolean pausiert() {
        return pausiert;
    }

    public void starten(Player spieler) {
        arena.world().getWorldBorder().setSize(arena.vollGroesse());
        if (bilder() > 0) {
            position = 0;
            anwenden(replay.bilder().get(0), true, true, true);
        }
        Location blick = mitte.clone().add(0, 9, -14);
        blick.setDirection(mitte.toVector().subtract(blick.toVector()));
        spieler.setGameMode(GameMode.SPECTATOR);
        spieler.teleport(blick);
        takt = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        Player spieler = Bukkit.getPlayer(zuschauer);
        if (spieler == null) {
            plugin.replays().beenden(zuschauer, false);
            return;
        }
        if (!pausiert) {
            vorrat += tempo;
            while (vorrat >= 1 && position < bilder() - 1) {
                vorrat -= 1;
                position++;
                boolean letztes = vorrat < 1 || position == bilder() - 1;
                anwenden(replay.bilder().get(position), letztes, tempo <= 2, letztes);
            }
            if (position >= bilder() - 1) {
                vorrat = 0;
                pausiert = true;
                if (!endeGezeigt) {
                    endeGezeigt = true;
                    endeZeigen(spieler);
                }
            }
        }
        if (anzeigeTakt++ % 5 == 0) {
            spieler.sendActionBar(plugin.msgs().format("replay-actionbar",
                    "zeit", zeit(position()), "gesamt", zeit(bilder() - 1),
                    "status", pausiert ? "❚❚" : "▶", "tempo", tempoText(tempo)));
        }
    }

    private void endeZeigen(Player spieler) {
        ReplayDaten.Ende ende = replay.ende();
        String titel;
        if (ende.ergebnis() == ReplayDaten.ERGEBNIS_SIEG && ende.gewinner() >= 0 && ende.gewinner() < figuren.length) {
            titel = plugin.msgs().raw("replay-ende-sieg").replace("%spieler%", replay.kopf().spieler().get(ende.gewinner()).name());
        } else if (ende.ergebnis() == ReplayDaten.ERGEBNIS_UNENTSCHIEDEN) {
            titel = plugin.msgs().raw("replay-ende-unentschieden");
        } else if (ende.ergebnis() == ReplayDaten.ERGEBNIS_AUFGABE) {
            titel = plugin.msgs().raw("replay-ende-aufgabe");
        } else {
            titel = plugin.msgs().raw("replay-ende-abgebrochen");
        }
        spieler.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(titel),
                plugin.msgs().format("replay-ende-unter")));
        plugin.msgs().send(spieler, "replay-ende");
    }

    public void pause(boolean an) {
        pausiert = an;
        if (!an && position >= bilder() - 1) {
            springenAuf(0);
            endeGezeigt = false;
        }
    }

    public void tempo(double neu) {
        tempo = Math.max(0.25, Math.min(4, neu));
    }

    public void springen(int ticks) {
        springenAuf(Math.max(0, Math.min(bilder() - 1, position() + ticks)));
        endeGezeigt = position >= bilder() - 1 && endeGezeigt;
    }

    public void springenAuf(int ziel) {
        if (bilder() == 0) {
            return;
        }
        int neu = Math.max(0, Math.min(bilder() - 1, ziel));
        if (neu < position) {
            zuruecksetzen();
        }
        while (position < neu) {
            position++;
            anwenden(replay.bilder().get(position), position == neu, false, position == neu);
        }
        vorrat = 0;
    }

    private void zuruecksetzen() {
        for (BlockState zustand : urspruenge.values()) {
            zustand.update(true, false);
        }
        urspruenge.clear();
        entities.values().forEach(ItemDisplay::remove);
        entities.clear();
        entityMaterial.clear();
        entityZiel.clear();
        for (int i = 0; i < figuren.length; i++) {
            ausruestung[i] = new byte[ReplayDaten.SLOTS][];
            if (figuren[i] != null && figuren[i].isValid()) {
                for (EquipmentSlot slot : SLOTS) {
                    figuren[i].getEquipment().setItem(slot, null);
                }
            }
        }
        arena.world().getWorldBorder().setSize(arena.vollGroesse());
        position = -1;
    }

    private Location ort(float x, float y, float z, float yaw, float pitch) {
        return new Location(arena.world(), mitte.getX() + x, mitte.getY() + y, mitte.getZ() + z, yaw, pitch);
    }

    private void anwenden(ReplayDaten.Bild bild, boolean bewegen, boolean effekte, boolean anzeigen) {
        for (ReplayDaten.Ereignis ereignis : bild.ereignisse()) {
            ereignis(ereignis, effekte);
        }
        if (!bewegen) {
            return;
        }
        for (int i = 0; i < figuren.length && i < bild.spieler().length; i++) {
            figurStellen(i, bild.spieler()[i], anzeigen);
        }
        for (Map.Entry<Integer, ItemDisplay> eintragDisplay : entities.entrySet()) {
            float[] ziel = entityZiel.get(eintragDisplay.getKey());
            if (ziel != null && eintragDisplay.getValue().isValid()) {
                eintragDisplay.getValue().teleport(ort(ziel[0], ziel[1], ziel[2], ziel[3], ziel[4]));
            }
        }
    }

    private Mannequin figur(int index) {
        Mannequin figur = figuren[index];
        if (figur != null && figur.isValid()) {
            return figur;
        }
        ReplayDaten.Teilnehmer teilnehmer = replay.kopf().spieler().get(index);
        figur = arena.world().spawn(mitte, Mannequin.class, m -> {
            PlayerProfile profil = Bukkit.createProfileExact(teilnehmer.id(), teilnehmer.name());
            if (!teilnehmer.skinWert().isEmpty()) {
                profil.setProperty(new ProfileProperty("textures", teilnehmer.skinWert(),
                        teilnehmer.skinSignatur().isEmpty() ? null : teilnehmer.skinSignatur()));
            }
            m.setProfile(ResolvableProfile.resolvableProfile(profil));
            m.setDescription(Component.text("Replay", NamedTextColor.DARK_GRAY));
            m.setImmovable(true);
            m.setGravity(false);
            m.setInvulnerable(true);
            m.setSilent(true);
            m.setPersistent(false);
            m.setCollidable(false);
            m.setCustomNameVisible(true);
        });
        figuren[index] = figur;
        for (int slot = 0; slot < ReplayDaten.SLOTS; slot++) {
            byte[] item = ausruestung[index][slot];
            figur.getEquipment().setItem(SLOTS[slot], gegenstand(item));
        }
        return figur;
    }

    private void figurStellen(int index, ReplayDaten.Zustand zustand, boolean anzeigen) {
        Mannequin figur = figur(index);
        if (!zustand.anwesend()) {
            figur.setInvisible(true);
            figur.setCustomNameVisible(false);
            return;
        }
        figur.setInvisible(false);
        figur.setCustomNameVisible(true);
        figur.teleport(ort(zustand.x(), zustand.y(), zustand.z(), zustand.yaw(), zustand.pitch()));
        figur.setBodyYaw(zustand.yaw());
        Pose pose = zustand.hat(ReplayDaten.GLEITEN) ? Pose.FALL_FLYING
                : zustand.hat(ReplayDaten.SCHWIMMEN) ? Pose.SWIMMING
                : zustand.hat(ReplayDaten.SCHLEICHEN) ? Pose.SNEAKING : Pose.STANDING;
        if (figur.getPose() != pose) {
            try {
                figur.setPose(pose, pose != Pose.STANDING);
            } catch (IllegalArgumentException ignoriert) {
            }
        }
        figur.setVisualFire(zustand.hat(ReplayDaten.BRENNT));
        figur.setGlowing(zustand.hat(ReplayDaten.UNSICHTBAR));
        if (anzeigen) {
            figur.customName(plugin.msgs().format("replay-name",
                    "spieler", replay.kopf().spieler().get(index).name(),
                    "leben", String.format(Locale.ROOT, "%.1f", zustand.leben())));
        }
    }

    private void ereignis(ReplayDaten.Ereignis ereignis, boolean effekte) {
        switch (ereignis) {
            case ReplayDaten.Schwung s -> {
                if (effekte && s.spieler() < figuren.length) {
                    Mannequin figur = figur(s.spieler());
                    if (s.nebenhand()) {
                        figur.swingOffHand();
                    } else {
                        figur.swingMainHand();
                    }
                }
            }
            case ReplayDaten.Schaden s -> {
                if (effekte && s.spieler() < figuren.length) {
                    Mannequin figur = figur(s.spieler());
                    figur.playHurtAnimation(figur.getLocation().getYaw());
                    Location ort = figur.getLocation().add(0, 1, 0);
                    arena.world().spawnParticle(Particle.CRIT, ort, 12, 0.3, 0.5, 0.3, 0.05);
                    zuschauerTon(ort, Sound.ENTITY_PLAYER_HURT);
                }
            }
            case ReplayDaten.Ausruestung a -> {
                if (a.spieler() < figuren.length && a.slot() < ReplayDaten.SLOTS) {
                    ausruestung[a.spieler()][a.slot()] = a.item().length == 0 ? null : a.item();
                    Mannequin figur = figuren[a.spieler()];
                    if (figur != null && figur.isValid()) {
                        figur.getEquipment().setItem(SLOTS[a.slot()], gegenstand(a.item()));
                    }
                }
            }
            case ReplayDaten.Block b -> blockSetzen(b);
            case ReplayDaten.EntityNeu n -> entityMaterial.put(n.id(), n.material());
            case ReplayDaten.EntityPos p -> entityBewegen(p);
            case ReplayDaten.EntityWeg w -> {
                entityMaterial.remove(w.id());
                entityZiel.remove(w.id());
                ItemDisplay display = entities.remove(w.id());
                if (display != null) {
                    display.remove();
                }
            }
            case ReplayDaten.Chat c -> {
                Player spieler = Bukkit.getPlayer(zuschauer);
                if (effekte && spieler != null && c.spieler() < figuren.length) {
                    spieler.sendMessage(plugin.msgs().format("replay-chat", "spieler",
                            replay.kopf().spieler().get(c.spieler()).name()).append(Component.text(c.text(), NamedTextColor.WHITE)));
                }
            }
            case ReplayDaten.Explosion e -> {
                if (effekte) {
                    Location ort = ort(e.x(), e.y(), e.z(), 0, 0);
                    arena.world().spawnParticle(Particle.EXPLOSION_EMITTER, ort, 1);
                    zuschauerTon(ort, Sound.ENTITY_GENERIC_EXPLODE);
                }
            }
            case ReplayDaten.Totem t -> {
                if (effekte && t.spieler() < figuren.length) {
                    Location ort = figur(t.spieler()).getLocation().add(0, 1, 0);
                    arena.world().spawnParticle(Particle.TOTEM_OF_UNDYING, ort, 60, 0.4, 0.8, 0.4, 0.3);
                    zuschauerTon(ort, Sound.ITEM_TOTEM_USE);
                }
            }
            case ReplayDaten.Grenze g -> arena.world().getWorldBorder().setSize(g.groesse(), effekte ? 1 : 0);
        }
    }

    private ItemStack gegenstand(byte[] daten) {
        if (daten == null || daten.length == 0) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(daten);
        } catch (RuntimeException fehler) {
            return null;
        }
    }

    private void zuschauerTon(Location ort, Sound ton) {
        Player spieler = Bukkit.getPlayer(zuschauer);
        if (spieler != null) {
            spieler.playSound(ort, ton, 0.8f, 1f);
        }
    }

    private void blockSetzen(ReplayDaten.Block b) {
        int x = mitte.getBlockX() + b.x();
        int y = mitte.getBlockY() + b.y();
        int z = mitte.getBlockZ() + b.z();
        World welt = arena.world();
        Block block = welt.getBlockAt(x, y, z);
        long schluessel = ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        urspruenge.putIfAbsent(schluessel, block.getState());
        try {
            block.setBlockData(Bukkit.createBlockData(b.daten()), false);
        } catch (IllegalArgumentException ignoriert) {
        }
    }

    private void entityBewegen(ReplayDaten.EntityPos p) {
        entityZiel.put(p.id(), new float[]{p.x(), p.y(), p.z(), p.yaw(), p.pitch()});
        ItemDisplay display = entities.get(p.id());
        if (display != null && display.isValid()) {
            return;
        }
        String materialName = entityMaterial.getOrDefault(p.id(), "BARRIER");
        Material material = Material.matchMaterial(materialName);
        ItemStack item = ItemStack.of(material != null && material.isItem() ? material : Material.BARRIER);
        float groesse = material == Material.END_CRYSTAL || material == Material.TNT ? 1f : 0.55f;
        ItemDisplay neu = arena.world().spawn(ort(p.x(), p.y(), p.z(), p.yaw(), p.pitch()), ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setBillboard(Display.Billboard.CENTER);
            d.setTeleportDuration(1);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(groesse, groesse, groesse), new AxisAngle4f()));
        });
        entities.put(p.id(), neu);
    }

    public void beenden() {
        if (beendet) {
            return;
        }
        beendet = true;
        if (takt != null) {
            takt.cancel();
        }
        for (BlockState zustand : urspruenge.values()) {
            zustand.update(true, false);
        }
        urspruenge.clear();
        entities.values().forEach(ItemDisplay::remove);
        entities.clear();
        for (Mannequin figur : figuren) {
            if (figur != null) {
                figur.remove();
            }
        }
        arena.world().getWorldBorder().setSize(arena.vollGroesse());
        plugin.arenaManager().freigeben(arena.name());
    }

    public void zuschauerZurueck(Player spieler) {
        spieler.setGameMode(vorherModus == GameMode.SPECTATOR ? GameMode.SURVIVAL : vorherModus);
        if (herkunft != null && !herkunft.equalsIgnoreCase(plugin.serverName())) {
            plugin.bridge().sende(spieler, herkunft);
        } else if (vorherOrt != null && vorherOrt.getWorld() != null && !vorherOrt.getWorld().equals(arena.world())) {
            spieler.teleport(vorherOrt);
        } else {
            spieler.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }

    public static String zeit(int ticks) {
        int sekunden = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%d:%02d", sekunden / 60, sekunden % 60);
    }

    public static String tempoText(double tempo) {
        return tempo == Math.floor(tempo) ? (int) tempo + "x" : String.valueOf(tempo).replace('.', ',') + "x";
    }

    public List<String> spielerNamen() {
        List<String> namen = new ArrayList<>();
        for (ReplayDaten.Teilnehmer t : replay.kopf().spieler()) {
            namen.add(t.name());
        }
        return namen;
    }
}

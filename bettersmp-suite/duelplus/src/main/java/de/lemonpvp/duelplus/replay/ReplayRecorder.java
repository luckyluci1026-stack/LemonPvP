package de.lemonpvp.duelplus.replay;

import com.destroystokyo.paper.profile.ProfileProperty;
import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.arena.Arena;
import de.lemonpvp.duelplus.session.DuellSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplayRecorder implements Listener {

    private static final int GRENZE_TAKT = 20;
    private static final int AUSRUESTUNG_TAKT = 2;

    private final DuelPlus plugin;
    private final ReplaySpeicher speicher;
    private final Map<String, Aufnahme> nachWelt = new ConcurrentHashMap<>();
    private final Map<UUID, Aufnahme> nachSpieler = new ConcurrentHashMap<>();
    private BukkitTask takt;

    public ReplayRecorder(DuelPlus plugin, ReplaySpeicher speicher) {
        this.plugin = plugin;
        this.speicher = speicher;
    }

    static final class Aufnahme {
        final String duellId;
        final String welt;
        final UUID[] spieler;
        final String[] namen;
        final double mx;
        final double my;
        final double mz;
        final long start;
        final ReplaySchreiber schreiber;
        final ItemStack[][] ausruestung;
        final Map<Integer, float[]> entities = new HashMap<>();
        final Set<Long> dirty = new LinkedHashSet<>();
        final int maxBilder;
        int tick;
        float grenze = -1;

        Aufnahme(String duellId, String welt, UUID[] spieler, String[] namen, Location mitte, long start,
                 ReplaySchreiber schreiber, int maxBilder) {
            this.duellId = duellId;
            this.welt = welt;
            this.spieler = spieler;
            this.namen = namen;
            this.mx = mitte.getX();
            this.my = mitte.getY();
            this.mz = mitte.getZ();
            this.start = start;
            this.schreiber = schreiber;
            this.ausruestung = new ItemStack[spieler.length][ReplayDaten.SLOTS];
            this.maxBilder = maxBilder;
        }

        int index(UUID id) {
            for (int i = 0; i < spieler.length; i++) {
                if (spieler[i].equals(id)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public boolean aktiv() {
        return plugin.getConfig().getBoolean("replay.aktiv", true);
    }

    public void starten() {
        if (takt == null) {
            takt = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    public void stoppen() {
        if (takt != null) {
            takt.cancel();
            takt = null;
        }
        for (Aufnahme aufnahme : List.copyOf(nachWelt.values())) {
            abschliessen(aufnahme, ReplayDaten.ERGEBNIS_ABGEBROCHEN, null, true);
        }
    }

    public boolean nimmtAuf(String welt) {
        return nachWelt.containsKey(welt);
    }

    public void aufnahmeStarten(DuellSession session, Arena arena, Player a, Player b) {
        if (!aktiv() || arena == null) {
            return;
        }
        Aufnahme alt = nachWelt.remove(arena.world().getName());
        if (alt != null) {
            abschliessen(alt, ReplayDaten.ERGEBNIS_ABGEBROCHEN, null, false);
        }
        Location mitte = arena.world().getSpawnLocation();
        long start = System.currentTimeMillis();
        List<ReplayDaten.Teilnehmer> teilnehmer = List.of(teilnehmer(a), teilnehmer(b));
        ReplayDaten.Kopf kopf = new ReplayDaten.Kopf(session.duellId(), arena.name(), mitte.getX(), mitte.getY(), mitte.getZ(),
                start, teilnehmer);
        int maxMinuten = Math.max(1, plugin.getConfig().getInt("replay.max-minuten", 30));
        Aufnahme aufnahme = new Aufnahme(session.duellId(), arena.world().getName(),
                new UUID[]{a.getUniqueId(), b.getUniqueId()}, new String[]{a.getName(), b.getName()},
                mitte, start, new ReplaySchreiber(kopf), maxMinuten * 60 * 20);
        nachWelt.put(aufnahme.welt, aufnahme);
        nachSpieler.put(a.getUniqueId(), aufnahme);
        nachSpieler.put(b.getUniqueId(), aufnahme);
        starten();
    }

    private static ReplayDaten.Teilnehmer teilnehmer(Player spieler) {
        String wert = "";
        String signatur = "";
        for (ProfileProperty eigenschaft : spieler.getPlayerProfile().getProperties()) {
            if (eigenschaft.getName().equals("textures")) {
                wert = eigenschaft.getValue();
                signatur = eigenschaft.getSignature() == null ? "" : eigenschaft.getSignature();
            }
        }
        return new ReplayDaten.Teilnehmer(spieler.getUniqueId(), spieler.getName(), wert, signatur);
    }

    public void aufnahmeBeenden(String duellId, int ergebnis, UUID gewinner) {
        for (Aufnahme aufnahme : List.copyOf(nachWelt.values())) {
            if (aufnahme.duellId.equals(duellId)) {
                bild(aufnahme);
                abschliessen(aufnahme, ergebnis, gewinner, false);
            }
        }
    }

    private void abschliessen(Aufnahme aufnahme, int ergebnis, UUID gewinner, boolean sofort) {
        nachWelt.remove(aufnahme.welt, aufnahme);
        for (UUID id : aufnahme.spieler) {
            nachSpieler.remove(id, aufnahme);
        }
        int gewinnerIndex = gewinner == null ? -1 : aufnahme.index(gewinner);
        byte[] daten = aufnahme.schreiber.beenden(ergebnis, gewinnerIndex);
        long dauer = aufnahme.tick * 50L;
        speicher.ablegen(new ReplaySpeicher.Fertig(aufnahme.duellId, aufnahme.spieler[0], aufnahme.namen[0],
                aufnahme.spieler[1], aufnahme.namen[1], aufnahme.welt, aufnahme.start, dauer, gewinner, ergebnis, daten), sofort);
    }

    public void tick() {
        for (Aufnahme aufnahme : List.copyOf(nachWelt.values())) {
            try {
                bild(aufnahme);
                if (aufnahme.tick >= aufnahme.maxBilder || aufnahme.schreiber.ungefaehreGroesse() > speicher.maxBytesProReplay()) {
                    abschliessen(aufnahme, ReplayDaten.ERGEBNIS_ABGEBROCHEN, null, false);
                    plugin.getLogger().warning("Replay " + aufnahme.duellId + " war zu lang/gross und wurde abgeschnitten.");
                }
            } catch (RuntimeException fehler) {
                plugin.getLogger().warning("Replay " + aufnahme.duellId + " konnte nicht weiter aufgenommen werden: " + fehler);
                abschliessen(aufnahme, ReplayDaten.ERGEBNIS_ABGEBROCHEN, null, false);
            }
        }
    }

    private void bild(Aufnahme aufnahme) {
        World welt = Bukkit.getWorld(aufnahme.welt);
        if (welt == null) {
            return;
        }
        ReplayDaten.Zustand[] zustaende = new ReplayDaten.Zustand[aufnahme.spieler.length];
        for (int i = 0; i < aufnahme.spieler.length; i++) {
            Player p = Bukkit.getPlayer(aufnahme.spieler[i]);
            zustaende[i] = p == null || !p.getWorld().equals(welt) ? ReplayDaten.Zustand.WEG : zustand(aufnahme, p);
        }
        aufnahme.schreiber.bild(zustaende);
        for (int i = 0; i < aufnahme.spieler.length; i++) {
            Player p = Bukkit.getPlayer(aufnahme.spieler[i]);
            if (p != null && p.getWorld().equals(welt) && (aufnahme.tick % AUSRUESTUNG_TAKT == 0 || aufnahme.tick == 0)) {
                ausruestungPruefen(aufnahme, i, p);
            }
        }
        if (aufnahme.tick % GRENZE_TAKT == 0) {
            float groesse = (float) welt.getWorldBorder().getSize();
            if (Math.abs(groesse - aufnahme.grenze) > 0.05f) {
                aufnahme.grenze = groesse;
                aufnahme.schreiber.grenze(groesse);
            }
        }
        bloeckeSchreiben(aufnahme, welt);
        entitiesSchreiben(aufnahme, welt);
        aufnahme.tick++;
    }

    private static ReplayDaten.Zustand zustand(Aufnahme aufnahme, Player p) {
        Location ort = p.getLocation();
        int flags = ReplayDaten.ANWESEND;
        if (p.isSneaking()) {
            flags |= ReplayDaten.SCHLEICHEN;
        }
        if (p.isSprinting()) {
            flags |= ReplayDaten.SPRINTEN;
        }
        if (p.isSwimming()) {
            flags |= ReplayDaten.SCHWIMMEN;
        }
        if (p.isBlocking()) {
            flags |= ReplayDaten.BLOCKEN;
        }
        if (p.getFireTicks() > 0) {
            flags |= ReplayDaten.BRENNT;
        }
        if (p.isInvisible()) {
            flags |= ReplayDaten.UNSICHTBAR;
        }
        if (p.isGliding()) {
            flags |= ReplayDaten.GLEITEN;
        }
        return new ReplayDaten.Zustand(true, (float) (ort.getX() - aufnahme.mx), (float) (ort.getY() - aufnahme.my),
                (float) (ort.getZ() - aufnahme.mz), ort.getYaw(), ort.getPitch(), flags, (float) p.getHealth());
    }

    private static ItemStack[] ausruestungVon(Player p) {
        EntityEquipment ausruestung = p.getEquipment();
        return new ItemStack[]{
                ausruestung.getItemInMainHand(), ausruestung.getItemInOffHand(), ausruestung.getHelmet(),
                ausruestung.getChestplate(), ausruestung.getLeggings(), ausruestung.getBoots()};
    }

    private static void ausruestungPruefen(Aufnahme aufnahme, int index, Player p) {
        ItemStack[] jetzt = ausruestungVon(p);
        for (int slot = 0; slot < ReplayDaten.SLOTS; slot++) {
            ItemStack neu = jetzt[slot] == null || jetzt[slot].isEmpty() ? null : vergleichbar(jetzt[slot]);
            ItemStack alt = aufnahme.ausruestung[index][slot];
            if (neu == null ? alt == null : alt != null && neu.isSimilar(alt)) {
                continue;
            }
            aufnahme.ausruestung[index][slot] = neu;
            aufnahme.schreiber.ausruestung(index, slot, neu == null ? null : neu.serializeAsBytes());
        }
    }

    static ItemStack vergleichbar(ItemStack item) {
        ItemStack kopie = item.clone();
        kopie.setAmount(1);
        if (kopie.getItemMeta() instanceof Damageable haltbarkeit && haltbarkeit.hasDamage()) {
            haltbarkeit.setDamage(0);
            kopie.setItemMeta(haltbarkeit);
        }
        return kopie;
    }

    private static long schluessel(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int[] ausSchluessel(long wert) {
        int x = (int) (wert >> 38);
        int z = (int) (wert << 26 >> 38);
        int y = (int) (wert << 52 >> 52);
        return new int[]{x, y, z};
    }

    private void bloeckeSchreiben(Aufnahme aufnahme, World welt) {
        if (aufnahme.dirty.isEmpty()) {
            return;
        }
        int bx = (int) Math.floor(aufnahme.mx);
        int by = (int) Math.floor(aufnahme.my);
        int bz = (int) Math.floor(aufnahme.mz);
        for (long wert : aufnahme.dirty) {
            int[] pos = ausSchluessel(wert);
            Block block = welt.getBlockAt(pos[0], pos[1], pos[2]);
            aufnahme.schreiber.block(pos[0] - bx, pos[1] - by, pos[2] - bz, block.getBlockData().getAsString());
        }
        aufnahme.dirty.clear();
    }

    private static String darstellung(Entity entity) {
        if (entity instanceof Player) {
            return null;
        }
        if (entity instanceof Item item) {
            return item.getItemStack().getType().name();
        }
        if (entity instanceof ThrowableProjectile wurf) {
            return wurf.getItem().getType().name();
        }
        if (entity instanceof Trident) {
            return "TRIDENT";
        }
        if (entity instanceof SpectralArrow) {
            return "SPECTRAL_ARROW";
        }
        if (entity instanceof AbstractArrow) {
            return "ARROW";
        }
        if (entity instanceof Firework) {
            return "FIREWORK_ROCKET";
        }
        if (entity instanceof EnderCrystal) {
            return "END_CRYSTAL";
        }
        if (entity instanceof TNTPrimed) {
            return "TNT";
        }
        if (entity instanceof FallingBlock fallend) {
            return fallend.getBlockData().getMaterial().name();
        }
        if (entity instanceof AbstractWindCharge) {
            return "WIND_CHARGE";
        }
        if (entity instanceof Fireball) {
            return "FIRE_CHARGE";
        }
        return null;
    }

    private static void entitiesSchreiben(Aufnahme aufnahme, World welt) {
        Set<Integer> gesehen = new HashSet<>();
        for (Entity entity : welt.getEntities()) {
            String material = darstellung(entity);
            if (material == null) {
                continue;
            }
            int id = entity.getEntityId();
            gesehen.add(id);
            Location ort = entity.getLocation();
            float x = (float) (ort.getX() - aufnahme.mx);
            float y = (float) (ort.getY() - aufnahme.my);
            float z = (float) (ort.getZ() - aufnahme.mz);
            float[] alt = aufnahme.entities.get(id);
            if (alt == null) {
                aufnahme.schreiber.entityNeu(id, material);
            } else if (Math.abs(alt[0] - x) < 0.01f && Math.abs(alt[1] - y) < 0.01f && Math.abs(alt[2] - z) < 0.01f
                    && Math.abs(alt[3] - ort.getYaw()) < 1f && Math.abs(alt[4] - ort.getPitch()) < 1f) {
                continue;
            }
            aufnahme.entities.put(id, new float[]{x, y, z, ort.getYaw(), ort.getPitch()});
            aufnahme.schreiber.entityPos(id, x, y, z, ort.getYaw(), ort.getPitch());
        }
        if (aufnahme.entities.size() == gesehen.size()) {
            return;
        }
        for (Integer id : new ArrayList<>(aufnahme.entities.keySet())) {
            if (!gesehen.contains(id)) {
                aufnahme.entities.remove(id);
                aufnahme.schreiber.entityWeg(id);
            }
        }
    }

    public void treffer(Player opfer, double schaden) {
        Aufnahme aufnahme = nachSpieler.get(opfer.getUniqueId());
        if (aufnahme != null) {
            aufnahme.schreiber.schaden(aufnahme.index(opfer.getUniqueId()), (float) schaden);
        }
    }

    public void chat(Player spieler, String text) {
        Aufnahme aufnahme = nachSpieler.get(spieler.getUniqueId());
        if (aufnahme != null) {
            int index = aufnahme.index(spieler.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (nachWelt.get(aufnahme.welt) == aufnahme) {
                    aufnahme.schreiber.chat(index, text);
                }
            });
        }
    }

    private Aufnahme fuerWelt(World welt) {
        return welt == null ? null : nachWelt.get(welt.getName());
    }

    private void merken(Block block) {
        Aufnahme aufnahme = fuerWelt(block.getWorld());
        if (aufnahme != null) {
            aufnahme.dirty.add(schluessel(block.getX(), block.getY(), block.getZ()));
        }
    }

    private void merken(List<Block> bloecke) {
        for (Block block : bloecke) {
            merken(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimSchwingen(PlayerAnimationEvent event) {
        Aufnahme aufnahme = nachSpieler.get(event.getPlayer().getUniqueId());
        if (aufnahme == null || event.isCancelled()) {
            return;
        }
        PlayerAnimationType art = event.getAnimationType();
        if (art == PlayerAnimationType.ARM_SWING || art == PlayerAnimationType.OFF_ARM_SWING) {
            aufnahme.schreiber.schwung(aufnahme.index(event.getPlayer().getUniqueId()), art == PlayerAnimationType.OFF_ARM_SWING);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimSchaden(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player spieler)) {
            return;
        }
        Aufnahme aufnahme = nachSpieler.get(spieler.getUniqueId());
        if (aufnahme == null) {
            return;
        }
        boolean toedlich = spieler.getHealth() - event.getFinalDamage() <= 0;
        if (event.isCancelled() && !toedlich) {
            return;
        }
        aufnahme.schreiber.schaden(aufnahme.index(spieler.getUniqueId()), (float) event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimTotem(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player spieler) {
            Aufnahme aufnahme = nachSpieler.get(spieler.getUniqueId());
            if (aufnahme != null) {
                aufnahme.schreiber.totem(aufnahme.index(spieler.getUniqueId()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimPlatzieren(BlockPlaceEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimAbbauen(BlockBreakEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beiEntityExplosion(EntityExplodeEvent event) {
        Aufnahme aufnahme = fuerWelt(event.getLocation().getWorld());
        if (aufnahme == null) {
            return;
        }
        merken(event.blockList());
        Location ort = event.getLocation();
        aufnahme.schreiber.explosion((float) (ort.getX() - aufnahme.mx), (float) (ort.getY() - aufnahme.my), (float) (ort.getZ() - aufnahme.mz));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beiBlockExplosion(BlockExplodeEvent event) {
        Aufnahme aufnahme = fuerWelt(event.getBlock().getWorld());
        if (aufnahme == null) {
            return;
        }
        merken(event.blockList());
        merken(event.getBlock());
        Block block = event.getBlock();
        aufnahme.schreiber.explosion((float) (block.getX() + 0.5 - aufnahme.mx), (float) (block.getY() + 0.5 - aufnahme.my),
                (float) (block.getZ() + 0.5 - aufnahme.mz));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimFliessen(BlockFromToEvent event) {
        merken(event.getToBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimEimerLeeren(PlayerBucketEmptyEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimEimerFuellen(PlayerBucketFillEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimBrennen(BlockBurnEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimEntzuenden(BlockIgniteEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimVerblassen(BlockFadeEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimEntstehen(BlockFormEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimAusbreiten(BlockSpreadEvent event) {
        merken(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimBlockWechsel(EntityChangeBlockEvent event) {
        merken(event.getBlock());
    }

    static long schluesselFuerTest(int x, int y, int z) {
        return schluessel(x, y, z);
    }

    static int[] ausSchluesselFuerTest(long wert) {
        return Arrays.copyOf(ausSchluessel(wert), 3);
    }
}

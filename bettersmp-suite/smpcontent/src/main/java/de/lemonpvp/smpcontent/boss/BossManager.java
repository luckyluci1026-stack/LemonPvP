package de.lemonpvp.smpcontent.boss;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.ability.Animator;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import de.lemonpvp.smpcontent.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Bosse: Mobs mit Lebensleiste, Phasen und eigenen Angriffen.
 *
 * Der Gedanke dahinter ist derselbe wie beim Rest des Plugins - im Code
 * steht kein einziger Boss, nur die Maschinerie. Was ein Boss ist, wieviel
 * er aushält, was er tut und wie es aussieht, steht komplett in der
 * bosse.yml. Die Angriffe greifen dabei auf die Formen aus der
 * animationen.yml zurück: Wer dort eine Zahl ändert, ändert damit auch,
 * wie der Boss aussieht, wenn er zuschlägt.
 *
 * Ein Kampf läuft so ab:
 *
 *   1. Der Boss wird gesetzt und bekommt seine Werte und Ausrüstung.
 *   2. Jeden Tick sucht er sich das nächste Ziel in Reichweite.
 *   3. Nach Lebensanteil gilt eine Phase; die bringt ihre eigenen Angriffe
 *      mit, jeder mit eigener Abklingzeit.
 *   4. Wer nah genug dran ist, sieht die Lebensleiste oben.
 *   5. Am Ende fällt die Beute, und es gibt einen letzten Auftritt.
 */
public final class BossManager {

    private final SMPContent plugin;
    private final NamespacedKey bossKey;
    private final NamespacedKey dienerKey;
    private final Map<String, BossType> types = new LinkedHashMap<>();
    private final Map<UUID, Fight> active = new LinkedHashMap<>();
    private ConfigProblem.Report problem;

    public BossManager(SMPContent plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "boss");
        this.dienerKey = new NamespacedKey(plugin, "boss_diener");
    }

    /** Ein laufender Kampf. */
    private static final class Fight {
        final BossType type;
        final LivingEntity mob;
        final BossBar bar;
        /** Angriff -> wann er wieder darf (Tick) */
        final Map<Integer, Integer> bereit = new HashMap<>();
        final List<UUID> diener = new ArrayList<>();
        String phase = "";
        int takt;
        /** Wann der letzte Phasenauftritt war - gegen Zappeln an der Schwelle. */
        int letzterWechsel = -999;

        Fight(BossType type, LivingEntity mob, BossBar bar) {
            this.type = type;
            this.mob = mob;
            this.bar = bar;
        }
    }

    // ------------------------------------------------------------------
    //  Laden
    // ------------------------------------------------------------------

    public void load() {
        types.clear();
        problem = null;
        File file = new File(plugin.getDataFolder(), "bosse.yml");
        if (!file.exists()) {
            plugin.saveResource("bosse.yml", false);
        }
        ConfigProblem.Result result = ConfigProblem.load(file);
        if (!result.ok()) {
            problem = result.problem();
            ConfigProblem.log(plugin.getLogger(), problem);
            return;
        }
        YamlConfiguration config = result.config();
        ConfigurationSection section = config.getConfigurationSection("bosse");
        if (section == null) {
            section = config.getConfigurationSection("bosses");
        }
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection one = section.getConfigurationSection(id);
            if (one != null) {
                String key = id.toLowerCase(Locale.ROOT);
                types.put(key, BossType.read(key, one));
            }
        }
    }

    public Map<String, BossType> types() {
        return types;
    }

    public ConfigProblem.Report problem() {
        return problem;
    }

    public int aktiv() {
        return active.size();
    }

    // ------------------------------------------------------------------
    //  Hinstellen
    // ------------------------------------------------------------------

    /** Setzt einen Boss. Gibt null zurück, wenn die Id unbekannt ist. */
    public LivingEntity spawn(String id, Location where) {
        BossType type = types.get(id.toLowerCase(Locale.ROOT));
        if (type == null || where.getWorld() == null) {
            return null;
        }
        EntityType kind;
        try {
            kind = EntityType.valueOf(type.mob());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Boss " + id + ": '" + type.mob()
                    + "' ist kein Mob-Typ.");
            return null;
        }
        Entity spawned = where.getWorld().spawnEntity(where, kind);
        if (!(spawned instanceof LivingEntity mob)) {
            spawned.remove();
            plugin.getLogger().warning("Boss " + id + ": " + type.mob()
                    + " ist nichts Lebendiges.");
            return null;
        }

        mob.customName(Text.mm(type.name()));
        mob.setCustomNameVisible(true);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setGlowing(type.glow());
        setzen(mob, Attribute.MAX_HEALTH, type.health());
        mob.setHealth(type.health());
        setzen(mob, Attribute.ARMOR, type.armor());
        setzen(mob, Attribute.MOVEMENT_SPEED, type.speed());
        setzen(mob, Attribute.KNOCKBACK_RESISTANCE, type.knockbackResist());
        setzen(mob, Attribute.SCALE, type.scale());
        setzen(mob, Attribute.FOLLOW_RANGE, 48.0);
        if (type.feuerfest()) {
            mob.setFireTicks(0);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
                    PotionEffect.INFINITE_DURATION, 0, false, false));
        }
        ausruesten(mob, type);
        mob.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, type.id());

        BossBar bar = Bukkit.createBossBar(
                Text.plain(type.name()), type.barColor(), type.barStyle(), BarFlag.DARKEN_SKY);
        bar.setProgress(1.0);
        Fight fight = new Fight(type, mob, bar);
        active.put(mob.getUniqueId(), fight);

        if (!type.spawnMessage().isBlank()) {
            for (Player nah : nahebei(mob.getLocation(), 64)) {
                nah.sendMessage(Text.mm(type.spawnMessage()));
            }
        }
        mob.getWorld().spawnParticle(Particle.EXPLOSION, mob.getLocation().add(0, 1, 0), 3);
        return mob;
    }

    private void setzen(LivingEntity mob, Attribute attribute, double wert) {
        var instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(wert);
        }
    }

    private void ausruesten(LivingEntity mob, BossType type) {
        var equip = mob.getEquipment();
        if (equip == null) {
            return;
        }
        type.gear().forEach((slot, id) -> {
            ItemStack item = itemOf(id);
            if (item == null) {
                return;
            }
            switch (slot) {
                case "hand", "hauptand", "mainhand" -> equip.setItemInMainHand(item);
                case "nebenhand", "offhand" -> equip.setItemInOffHand(item);
                case "kopf", "helm", "head" -> equip.setHelmet(item);
                case "brust", "chest" -> equip.setChestplate(item);
                case "hose", "legs" -> equip.setLeggings(item);
                case "schuhe", "feet" -> equip.setBoots(item);
                default -> { }
            }
        });
        // Nichts von der Ausrüstung fällt zufällig herunter - was es gibt,
        // steht unter "beute".
        equip.setItemInMainHandDropChance(0);
        equip.setItemInOffHandDropChance(0);
        equip.setHelmetDropChance(0);
        equip.setChestplateDropChance(0);
        equip.setLeggingsDropChance(0);
        equip.setBootsDropChance(0);
    }

    /** "smp:frost_blade" oder "DIAMOND_SWORD" - beides geht. */
    private ItemStack itemOf(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String clean = id.startsWith("smp:") ? id.substring(4) : id;
        CustomEntry entry = plugin.registry().get(clean);
        if (entry != null) {
            return plugin.registry().create(entry, 1);
        }
        Material material = Material.matchMaterial(id);
        return material == null ? null : new ItemStack(material);
    }

    // ------------------------------------------------------------------
    //  Der Kampf
    // ------------------------------------------------------------------

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    private void tick() {
        if (active.isEmpty()) {
            return;
        }
        List<UUID> weg = new ArrayList<>();
        for (Map.Entry<UUID, Fight> eintrag : new ArrayList<>(active.entrySet())) {
            Fight fight = eintrag.getValue();
            if (!fight.mob.isValid() || fight.mob.isDead()) {
                weg.add(eintrag.getKey());
                continue;
            }
            kaempfen(fight);
        }
        weg.forEach(id -> {
            Fight fight = active.remove(id);
            if (fight != null) {
                fight.bar.removeAll();
            }
        });
    }

    private void kaempfen(Fight fight) {
        fight.takt++;
        LivingEntity mob = fight.mob;
        double max = maxLeben(mob);
        double anteil = Math.max(0, Math.min(1, mob.getHealth() / max));

        // Lebensleiste: nur für die, die auch dabei sind
        if (fight.takt % 10 == 0) {
            fight.bar.setProgress(anteil);
            List<Player> nah = nahebei(mob.getLocation(), 48);
            fight.bar.removeAll();
            nah.forEach(fight.bar::addPlayer);
        }

        BossType.Phase phase = fight.type.phaseFor(anteil);
        if (!phase.name().equals(fight.phase)) {
            // Ein Boss, der sich heilt, rutscht in die vorige Phase zurück -
            // das ist so gewollt. Ohne Sperre gäbe es aber bei jedem Zittern
            // um die Schwelle wieder Ansage, Ton und Animation.
            boolean ersteMal = fight.phase.isEmpty();
            fight.phase = phase.name();
            if (ersteMal || fight.takt - fight.letzterWechsel >= 60) {
                fight.letzterWechsel = fight.takt;
                phasenwechsel(fight, phase);
            }
        }

        Player ziel = naechster(mob.getLocation(), 32);
        if (ziel == null) {
            return;
        }
        if (mob instanceof Mob gegner && gegner.getTarget() == null) {
            gegner.setTarget(ziel);
        }

        for (int index = 0; index < phase.attacks().size(); index++) {
            BossType.Attack attack = phase.attacks().get(index);
            int schluessel = phase.name().hashCode() * 31 + index;
            int frei = fight.bereit.getOrDefault(schluessel, 0);
            if (fight.takt < frei) {
                continue;
            }
            if (mob.getLocation().distance(ziel.getLocation()) > attack.range() + 4) {
                continue;
            }
            fight.bereit.put(schluessel,
                    fight.takt + Math.max(10, (int) (attack.cooldown() * 20)));
            angreifen(fight, attack, ziel);
        }
    }

    private void phasenwechsel(Fight fight, BossType.Phase phase) {
        LivingEntity mob = fight.mob;
        if (!phase.name().isBlank()) {
            fight.bar.setTitle(Text.plain(fight.type.name() + " §7- §f" + phase.name()));
            for (Player nah : nahebei(mob.getLocation(), 48)) {
                nah.sendActionBar(Text.mm("<red><bold>" + phase.name()));
            }
        }
        if (!phase.enterSound().isBlank()) {
            mob.getWorld().playSound(mob.getLocation(), phase.enterSound(), 1.4f, 0.8f);
        }
        zeigen(mob.getLocation(), phase.enterAnimation(), phase.enterParticle(),
                phase.enterColor(), 5.0, 8.0, 25);
        mob.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, mob.getLocation(), 1);
    }

    // ------------------------------------------------------------------
    //  Die Angriffe
    // ------------------------------------------------------------------

    private void angreifen(Fight fight, BossType.Attack attack, Player ziel) {
        LivingEntity mob = fight.mob;
        Location wo = mob.getLocation();
        if (!attack.sound().isBlank()) {
            mob.getWorld().playSound(wo, attack.sound(), 1.2f, 1.0f);
        }
        if (!attack.message().isBlank()) {
            for (Player nah : nahebei(wo, 32)) {
                nah.sendActionBar(Text.mm(attack.message()));
            }
        }

        switch (attack.type()) {
            // Ring um den Boss: alles im Umkreis bekommt etwas ab
            case "welle", "shockwave" -> {
                zeigen(wo, attack.animation(), attack.particle(), attack.color(),
                        attack.range(), attack.range(), 20);
                for (Player p : nahebei(wo, attack.range())) {
                    treffen(mob, p, attack.damage());
                    p.setVelocity(p.getLocation().toVector().subtract(wo.toVector())
                            .normalize().multiply(attack.value()).setY(0.45));
                }
            }
            // Strahl auf das Ziel zu - trifft alles auf der Linie
            case "strahl", "beam" -> {
                Vector richtung = ziel.getLocation().toVector()
                        .subtract(wo.toVector()).normalize();
                Location blick = wo.clone().add(0, mob.getHeight() * 0.6, 0);
                blick.setDirection(richtung);
                zeigen(blick, attack.animation(), attack.particle(), attack.color(),
                        1.5, attack.range(), 15);
                for (Player p : nahebei(wo, attack.range() + 2)) {
                    Vector zuP = p.getLocation().add(0, 1, 0).toVector()
                            .subtract(blick.toVector());
                    if (zuP.length() <= attack.range()
                            && zuP.normalize().dot(richtung) > 0.94) {
                        treffen(mob, p, attack.damage());
                    }
                }
            }
            // Sprung auf das Ziel
            case "sprung", "leap" -> {
                Vector hin = ziel.getLocation().toVector().subtract(wo.toVector());
                hin.setY(0).normalize().multiply(Math.max(0.4, attack.value()));
                hin.setY(0.55);
                mob.setVelocity(hin);
                zeigen(wo, attack.animation(), attack.particle(), attack.color(),
                        2.0, 4.0, 12);
            }
            // Zieht alle heran - kein Weglaufen
            case "zug", "pull" -> {
                zeigen(wo, attack.animation(), attack.particle(), attack.color(),
                        attack.range(), attack.range(), 20);
                for (Player p : nahebei(wo, attack.range())) {
                    p.setVelocity(wo.toVector().subtract(p.getLocation().toVector())
                            .normalize().multiply(Math.max(0.3, attack.value())));
                }
            }
            // Meteore, die um das Ziel herum einschlagen
            case "meteor" -> meteore(fight, attack, ziel);
            // Diener rufen
            case "diener", "adds", "summon" -> diener(fight, attack, ziel);
            // Trank auf alle in Reichweite
            case "trank", "potion" -> {
                PotionEffectType effekt = trankTyp(attack.potion());
                zeigen(wo, attack.animation(), attack.particle(), attack.color(),
                        attack.range(), attack.range(), 20);
                if (effekt == null) {
                    return;
                }
                for (Player p : nahebei(wo, attack.range())) {
                    p.addPotionEffect(new PotionEffect(effekt,
                            attack.potionSeconds() * 20, Math.max(0, attack.potionLevel() - 1)));
                }
            }
            // Sich selbst heilen
            case "heilen", "heal" -> {
                double neu = Math.min(maxLeben(mob), mob.getHealth() + attack.value());
                mob.setHealth(neu);
                zeigen(wo, attack.animation(), attack.particle(), attack.color(),
                        3.0, 5.0, 20);
            }
            // Geschosse auf das Ziel
            case "geschoss", "projectile" -> geschosse(fight, attack, ziel);
            default -> plugin.getLogger().warning("Boss " + fight.type.id()
                    + ": unbekannter Angriff '" + attack.type() + "'");
        }
    }

    private void meteore(Fight fight, BossType.Attack attack, Player ziel) {
        LivingEntity mob = fight.mob;
        for (int i = 0; i < Math.max(1, attack.count()); i++) {
            double winkel = Math.random() * Math.PI * 2;
            double weite = Math.random() * attack.range();
            Location einschlag = ziel.getLocation().clone().add(
                    Math.cos(winkel) * weite, 0, Math.sin(winkel) * weite);
            int verzoegerung = 10 + i * 6;
            // Erst die Warnung am Boden, dann der Einschlag - sonst hat
            // niemand eine Chance auszuweichen.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                einschlag.getWorld().spawnParticle(Particle.DUST,
                        einschlag.clone().add(0, 0.2, 0), 30, 1.2, 0.05, 1.2, 0,
                        new Particle.DustOptions(
                                Animator.parseColor(attack.color()), 1.6f));
            }, Math.max(1, verzoegerung - 8));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!mob.isValid()) {
                    return;
                }
                zeigen(einschlag, attack.animation(), attack.particle(), attack.color(),
                        2.5, 6.0, 15);
                einschlag.getWorld().spawnParticle(Particle.EXPLOSION, einschlag, 1);
                einschlag.getWorld().playSound(einschlag,
                        org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.3f);
                for (Player p : nahebei(einschlag, 2.6)) {
                    treffen(mob, p, attack.damage());
                }
            }, verzoegerung);
        }
    }

    private void diener(Fight fight, BossType.Attack attack, Player ziel) {
        LivingEntity mob = fight.mob;
        EntityType kind;
        try {
            kind = EntityType.valueOf(attack.mob().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Boss " + fight.type.id() + ": '" + attack.mob()
                    + "' ist kein Mob-Typ.");
            return;
        }
        // Nicht endlos: wer alle sechs Sekunden vier Diener ruft, macht den
        // Server platt, nicht den Kampf spannend.
        fight.diener.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            return e == null || !e.isValid();
        });
        int platz = Math.max(0, 12 - fight.diener.size());
        for (int i = 0; i < Math.min(platz, Math.max(1, attack.count())); i++) {
            Location wo = mob.getLocation().clone().add(
                    (Math.random() - 0.5) * 4, 0.5, (Math.random() - 0.5) * 4);
            Entity diener = wo.getWorld().spawnEntity(wo, kind);
            diener.getPersistentDataContainer()
                    .set(dienerKey, PersistentDataType.STRING, fight.type.id());
            if (diener instanceof Mob m) {
                m.setTarget(ziel);
                var leben = m.getAttribute(Attribute.MAX_HEALTH);
                if (leben != null && attack.value() > 0) {
                    leben.setBaseValue(attack.value());
                    m.setHealth(attack.value());
                }
            }
            fight.diener.add(diener.getUniqueId());
            wo.getWorld().spawnParticle(Particle.LARGE_SMOKE, wo, 12, 0.3, 0.3, 0.3, 0.02);
        }
        zeigen(mob.getLocation(), attack.animation(), attack.particle(), attack.color(),
                3.0, 5.0, 20);
    }

    private void geschosse(Fight fight, BossType.Attack attack, Player ziel) {
        LivingEntity mob = fight.mob;
        Location aus = mob.getLocation().clone().add(0, mob.getHeight() * 0.7, 0);
        for (int i = 0; i < Math.max(1, attack.count()); i++) {
            Vector hin = ziel.getEyeLocation().toVector().subtract(aus.toVector()).normalize();
            hin.add(new Vector((Math.random() - 0.5) * 0.18,
                    (Math.random() - 0.5) * 0.18, (Math.random() - 0.5) * 0.18));
            var kugel = mob.getWorld().spawn(aus.clone().add(hin.clone().multiply(1.2)),
                    org.bukkit.entity.SmallFireball.class);
            kugel.setShooter(mob);
            kugel.setVelocity(hin.normalize().multiply(Math.max(0.6, attack.value())));
            kugel.setIsIncendiary(false);
            kugel.setYield(0);
        }
        zeigen(aus, attack.animation(), attack.particle(), attack.color(), 2.0, 4.0, 12);
    }

    /**
     * Schaden vom Boss.
     *
     * Wie bei den Kugeln wird die Unverwundbarkeit vorher aufgehoben: Ein
     * Boss, der zwei Angriffe kurz hintereinander landet, soll auch zweimal
     * treffen und nicht einmal.
     */
    private void treffen(LivingEntity von, Player wen, double schaden) {
        if (schaden <= 0 || wen.isDead() || wen.getGameMode() == org.bukkit.GameMode.CREATIVE
                || wen.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        wen.setNoDamageTicks(0);
        wen.setLastDamage(0);
        wen.damage(schaden, von);
    }

    /** Spielt eine Form aus der animationen.yml an diesem Ort ab. */
    private void zeigen(Location wo, String form, String teilchen, String farbe,
                        double radius, double laenge, int ticks) {
        if (form == null || form.isBlank()) {
            return;
        }
        var animation = plugin.abilities().animation(form);
        if (animation == null) {
            plugin.getLogger().warning("Boss-Animation '" + form
                    + "' steht nicht in der animationen.yml.");
            return;
        }
        Particle particle = Animator.particleByName(teilchen);
        if (particle == null) {
            particle = Particle.DUST;
        }
        Object data = Animator.dataFor(particle, farbe, 1.2);
        Location fest = wo.clone();
        plugin.abilities().animator().playCustom(() -> fest, 1.0,
                animation, particle, data, radius, laenge, ticks);
    }

    // ------------------------------------------------------------------
    //  Ende
    // ------------------------------------------------------------------

    /** Ist diese Kreatur einer unserer Bosse? */
    public boolean isBoss(Entity entity) {
        return entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING);
    }

    /** Der Boss ist gefallen: Beute, Ansage, letzter Auftritt. */
    public void onDeath(LivingEntity mob, List<ItemStack> drops) {
        Fight fight = active.remove(mob.getUniqueId());
        if (fight == null) {
            return;
        }
        fight.bar.removeAll();
        BossType type = fight.type;
        drops.clear();
        for (BossType.Drop drop : type.drops()) {
            if (Math.random() > drop.chance()) {
                continue;
            }
            ItemStack item = itemOf(drop.item());
            if (item == null) {
                continue;
            }
            int menge = drop.min() + (int) (Math.random() * (Math.max(drop.min(),
                    drop.max()) - drop.min() + 1));
            item.setAmount(Math.max(1, menge));
            drops.add(item);
        }
        // Die Diener gehen mit ihrem Herrn
        for (UUID id : fight.diener) {
            Entity diener = Bukkit.getEntity(id);
            if (diener != null && diener.isValid()) {
                diener.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        diener.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
                diener.remove();
            }
        }
        Location wo = mob.getLocation();
        wo.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, wo.clone().add(0, 1, 0), 3);
        wo.getWorld().playSound(wo, org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH, 1.2f, 1.4f);
        if (!type.deathMessage().isBlank()) {
            for (Player nah : nahebei(wo, 64)) {
                nah.sendMessage(Text.mm(type.deathMessage()));
            }
        }
    }

    /** Beim Abschalten die Leisten wegräumen - sonst kleben sie am Bildschirm. */
    public void clear() {
        active.values().forEach(fight -> fight.bar.removeAll());
        active.clear();
    }

    // ------------------------------------------------------------------
    //  Kleinkram
    // ------------------------------------------------------------------

    private double maxLeben(LivingEntity mob) {
        var attribut = mob.getAttribute(Attribute.MAX_HEALTH);
        return attribut == null ? 20 : attribut.getValue();
    }

    private List<Player> nahebei(Location wo, double weite) {
        List<Player> gefunden = new ArrayList<>();
        if (wo.getWorld() == null) {
            return gefunden;
        }
        for (Player p : wo.getWorld().getPlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }
            if (p.getLocation().distanceSquared(wo) <= weite * weite) {
                gefunden.add(p);
            }
        }
        return gefunden;
    }

    private Player naechster(Location wo, double weite) {
        Player beste = null;
        double kuerzeste = weite * weite;
        for (Player p : nahebei(wo, weite)) {
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                continue;
            }
            double abstand = p.getLocation().distanceSquared(wo);
            if (abstand <= kuerzeste) {
                kuerzeste = abstand;
                beste = p;
            }
        }
        return beste;
    }

    private PotionEffectType trankTyp(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return org.bukkit.Registry.EFFECT.get(
                NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).replace(' ', '_')));
    }
}

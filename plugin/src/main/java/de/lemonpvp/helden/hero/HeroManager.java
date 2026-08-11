package de.lemonpvp.helden.hero;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.config.ConfigFile;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Laedt die Heldenklassen und wendet Kit sowie Passivwerte an. */
public final class HeroManager {

    /** Letzter Slot, den {@link PlayerInventory#setItem(int, ItemStack)} akzeptiert (40 = Zweithand). */
    private static final int MAX_INVENTORY_SLOT = 40;

    private final HeldenPlugin plugin;
    private final ConfigFile file;
    private final Map<String, Hero> heroes = new LinkedHashMap<>();

    public HeroManager(HeldenPlugin plugin) {
        this.plugin = plugin;
        this.file = new ConfigFile(plugin, "heroes.yml");
    }

    public void reload() {
        heroes.clear();
        file.reload();

        ConfigurationSection root = file.get().getConfigurationSection("heroes");
        if (root == null) {
            plugin.getLogger().warning("heroes.yml enthaelt keinen 'heroes'-Abschnitt.");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Hero hero = read(id.toLowerCase(Locale.ROOT), section);
            if (hero != null) {
                heroes.put(hero.id(), hero);
            }
        }

        plugin.getLogger().info("Helden geladen: " + heroes.size());
    }

    private Hero read(String id, ConfigurationSection section) {
        Material icon = Compat.material(section.getString("icon", "PAPER"));
        if (icon == null) {
            plugin.getLogger().warning("Held '" + id + "' hat ein unbekanntes Icon, nutze PAPER.");
            icon = Material.PAPER;
        }

        List<Hero.Passive> passives = new ArrayList<>();
        for (String raw : section.getStringList("passives.effects")) {
            Hero.Passive passive = readPassive(id, raw);
            if (passive != null) {
                passives.add(passive);
            }
        }

        return new Hero(
                id,
                section.getString("display", id),
                icon,
                section.getInt("order", 99),
                section.getStringList("description"),
                section.getString("ability", ""),
                section.getString("weapon", ""),
                passives,
                section.getDouble("passives.damage-dealt-multiplier", 1.0),
                section.getDouble("passives.damage-taken-multiplier", 1.0),
                section.getDouble("passives.projectile-damage-percent", 0.0),
                readKit(id, section));
    }

    private Hero.Passive readPassive(String heroId, String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String[] parts = raw.split(":");
        PotionEffectType type = Compat.effect(parts[0]);
        if (type == null) {
            plugin.getLogger().warning("Held '" + heroId + "': unbekannter Effekt '" + parts[0] + "'.");
            return null;
        }
        int amplifier = 0;
        if (parts.length > 1) {
            try {
                amplifier = Math.max(0, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Held '" + heroId + "': '" + parts[1] + "' ist keine Effektstaerke.");
            }
        }
        return new Hero.Passive(type, amplifier);
    }

    private List<KitEntry> readKit(String heroId, ConfigurationSection section) {
        List<KitEntry> kit = new ArrayList<>();
        for (Map<?, ?> entry : section.getMapList("kit")) {
            Object itemId = entry.get("item");
            Object materialName = entry.get("material");
            int amount = toInt(entry.get("amount"), 1);
            int slot = toInt(entry.get("slot"), -1);

            if (itemId != null) {
                kit.add(new KitEntry(String.valueOf(itemId), null, amount, slot));
                continue;
            }
            if (materialName == null) {
                plugin.getLogger().warning("Held '" + heroId + "': Kit-Eintrag ohne 'item' oder 'material'.");
                continue;
            }
            Material material = Compat.material(String.valueOf(materialName));
            if (material == null) {
                plugin.getLogger().warning("Held '" + heroId + "': unbekanntes Material '" + materialName + "'.");
                continue;
            }
            kit.add(new KitEntry(null, material, amount, slot));
        }
        return kit;
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public Hero get(String id) {
        return id == null ? null : heroes.get(id.toLowerCase(Locale.ROOT));
    }

    public Hero of(HeldenProfile profile) {
        return profile == null ? null : get(profile.heroId());
    }

    public Hero of(Player player) {
        return of(plugin.profiles().get(player));
    }

    public List<Hero> sorted() {
        List<Hero> list = new ArrayList<>(heroes.values());
        list.sort(Comparator.comparingInt(Hero::order).thenComparing(Hero::id));
        return list;
    }

    public Set<String> ids() {
        return new LinkedHashSet<>(heroes.keySet());
    }

    public int size() {
        return heroes.size();
    }

    /** Sekunden bis zum naechsten erlaubten Heldenwechsel (0 = jetzt erlaubt). */
    public long changeRemaining(HeldenProfile profile) {
        int cooldown = plugin.settings().heroChangeCooldownSeconds();
        if (cooldown < 0 || profile == null || !profile.hasHero()) {
            return 0L;
        }
        long elapsed = (System.currentTimeMillis() - profile.heroSelectedAt()) / 1000L;
        return Math.max(0L, cooldown - elapsed);
    }

    /** {@code false}, wenn ein Wechsel grundsaetzlich verboten ist. */
    public boolean changeEverAllowed(HeldenProfile profile) {
        return plugin.settings().heroChangeCooldownSeconds() != 0 || profile == null || !profile.hasHero();
    }

    /** Darf dieser Spieler gerade wechseln? */
    public boolean canChangeNow(Player player, HeldenProfile profile) {
        if (player.hasPermission("helden3.hero.change.anytime")) {
            return true;
        }
        if (profile == null || !profile.hasHero()) {
            return true;
        }
        return changeEverAllowed(profile) && changeRemaining(profile) <= 0;
    }

    /**
     * Kompletter Auswahlvorgang inklusive Pruefungen und Meldungen - genutzt von
     * {@code /held <name>} und vom Auswahlmenue.
     *
     * @return {@code true}, wenn der Held gewechselt wurde
     */
    public boolean trySelect(Player player, Hero hero) {
        HeldenProfile profile = plugin.profiles().getOrCreate(player);

        if (hero.id().equalsIgnoreCase(profile.heroId())) {
            plugin.messages().send(player, "hero.already-selected", "%hero%", hero.display());
            return false;
        }
        if (!canChangeNow(player, profile)) {
            if (!changeEverAllowed(profile)) {
                plugin.messages().send(player, "hero.change-disabled");
            } else {
                plugin.messages().send(player, "hero.change-cooldown",
                        "%time%", TimeUtil.format(changeRemaining(profile)));
            }
            Compat.sound(player, "block.note_block.bass", 0.7f, 0.8f);
            return false;
        }

        apply(player, hero);
        plugin.messages().send(player, "hero.selected", "%hero%", hero.display());
        plugin.messages().broadcastRaw("hero.broadcast",
                "%player%", player.getName(),
                "%hero%", hero.display());
        plugin.abilities().sendHint(player, hero);
        Compat.sound(player, "entity.player.levelup", 1.0f, 1.2f);
        return true;
    }

    /** Setzt den Helden, gibt das Kit aus und aktualisiert alle Ableitungen. */
    public void apply(Player player, Hero hero) {
        HeldenProfile profile = plugin.profiles().getOrCreate(player);
        boolean changed = !hero.id().equalsIgnoreCase(profile.heroId());

        profile.heroId(hero.id());
        profile.heroSelectedAt(System.currentTimeMillis());

        if (changed && plugin.settings().clearInventoryOnChange()) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }

        giveKit(player, hero);
        clearPassives(player);
        applyPassives(player, hero);
        plugin.abilities().clear(player);
        plugin.hud().update(player);
    }

    public void giveKit(Player player, Hero hero) {
        if (hero == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (KitEntry entry : hero.kit()) {
            ItemStack stack = toStack(entry);
            if (stack == null) {
                continue;
            }
            if (entry.slot() >= 0 && entry.slot() <= MAX_INVENTORY_SLOT) {
                inventory.setItem(entry.slot(), stack);
            } else {
                inventory.addItem(stack);
            }
        }
    }

    private ItemStack toStack(KitEntry entry) {
        if (entry.isCustomItem()) {
            ItemStack stack = plugin.items().stack(entry.itemId(), entry.amount());
            if (stack == null) {
                plugin.getLogger().warning("Kit verweist auf das unbekannte Artefakt '" + entry.itemId() + "'.");
            }
            return stack;
        }
        return entry.material() == null ? null : new ItemStack(entry.material(), Math.max(1, entry.amount()));
    }

    /** Frischt die passiven Effekte des aktuellen Helden auf. */
    public void applyPassives(Player player) {
        applyPassives(player, of(player));
    }

    public void applyPassives(Player player, Hero hero) {
        if (hero == null || hero.passives().isEmpty()) {
            return;
        }
        // Etwas laenger als das Refresh-Intervall, damit nichts flackert.
        int duration = (plugin.settings().passiveRefreshSeconds() + 3) * 20;
        for (Hero.Passive passive : hero.passives()) {
            player.addPotionEffect(passive.toEffect(duration));
        }
    }

    /** Entfernt alle Effekte, die irgendein Held als Passiv vergibt. */
    public void clearPassives(Player player) {
        for (Hero hero : heroes.values()) {
            for (Hero.Passive passive : hero.passives()) {
                player.removePotionEffect(passive.type());
            }
        }
    }
}

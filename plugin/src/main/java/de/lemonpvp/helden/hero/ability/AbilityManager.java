package de.lemonpvp.helden.hero.ability;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.config.Settings;
import de.lemonpvp.helden.hero.Hero;
import de.lemonpvp.helden.hero.ability.impl.FeuerballAbility;
import de.lemonpvp.helden.hero.ability.impl.HeilkreisAbility;
import de.lemonpvp.helden.hero.ability.impl.PfeilhagelAbility;
import de.lemonpvp.helden.hero.ability.impl.SchattenschrittAbility;
import de.lemonpvp.helden.hero.ability.impl.SchildwallAbility;
import de.lemonpvp.helden.hero.ability.impl.WirbelsturmAbility;
import de.lemonpvp.helden.item.CustomItem;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Cooldowns;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Registry und Ausloeselogik der Heldenfaehigkeiten. */
public final class AbilityManager {

    private final HeldenPlugin plugin;
    private final Map<String, Ability> abilities = new LinkedHashMap<>();
    private final Cooldowns cooldowns = new Cooldowns();

    public AbilityManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
        abilities.clear();
        register(new WirbelsturmAbility(plugin));
        register(new PfeilhagelAbility(plugin));
        register(new SchattenschrittAbility(plugin));
        register(new FeuerballAbility(plugin));
        register(new HeilkreisAbility(plugin));
        register(new SchildwallAbility(plugin));
        plugin.getLogger().info("Faehigkeiten registriert: " + abilities.size());
    }

    public void register(Ability ability) {
        abilities.put(ability.id().toLowerCase(Locale.ROOT), ability);
    }

    public Ability get(String id) {
        return id == null ? null : abilities.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return abilities.keySet();
    }

    public int size() {
        return abilities.size();
    }

    /** Erklaert dem Spieler, wie er seine Faehigkeit ausloest. */
    public void sendHint(Player player, Hero hero) {
        if (hero == null || !hero.hasAbility()) {
            return;
        }
        Ability ability = get(hero.abilityId());
        if (ability == null) {
            return;
        }
        String triggerPath = plugin.settings().abilityTrigger() == Settings.AbilityTrigger.SNEAK_RIGHT_CLICK
                ? "ability.trigger-sneak"
                : "ability.trigger-right-click";
        CustomItem weapon = plugin.items().get(hero.weaponId());
        plugin.messages().send(player, "ability.hint",
                "%ability%", ability.display(),
                "%trigger%", plugin.messages().raw(triggerPath),
                "%weapon%", weapon == null ? "&f-" : weapon.display());
    }

    public long remaining(Player player, Ability ability) {
        return cooldowns.remaining(player.getUniqueId(), ability.id());
    }

    public void clear(Player player) {
        cooldowns.clear(player.getUniqueId());
    }

    /**
     * Loest die Faehigkeit des aktuellen Helden aus und kuemmert sich um
     * Cooldown, Rueckmeldung und Sound.
     */
    public boolean trigger(Player player) {
        HeldenProfile profile = plugin.profiles().get(player);
        if (profile == null) {
            return false;
        }
        if (profile.fallen()) {
            plugin.messages().send(player, "ability.dead");
            return false;
        }

        Hero hero = plugin.heroes().of(profile);
        if (hero == null) {
            plugin.messages().send(player, "ability.no-hero");
            return false;
        }
        if (!hero.hasAbility()) {
            plugin.messages().send(player, "ability.no-ability");
            return false;
        }

        Ability ability = get(hero.abilityId());
        if (ability == null) {
            plugin.getLogger().warning("Held '" + hero.id() + "' verweist auf die unbekannte Faehigkeit '"
                    + hero.abilityId() + "'.");
            plugin.messages().send(player, "ability.no-ability");
            return false;
        }

        long remaining = cooldowns.remaining(player.getUniqueId(), ability.id());
        if (remaining > 0) {
            plugin.messages().send(player, "ability.cooldown",
                    "%ability%", ability.display(),
                    "%seconds%", remaining);
            Compat.sound(player, "block.note_block.bass", 0.6f, 0.8f);
            return false;
        }

        if (!ability.execute(player)) {
            plugin.messages().send(player, "ability.failed");
            return false;
        }

        cooldowns.set(player.getUniqueId(), ability.id(), ability.cooldownSeconds());
        plugin.messages().send(player, "ability.used", "%ability%", ability.display());
        return true;
    }
}

package de.lemonpvp.helden.ui;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.Hero;
import de.lemonpvp.helden.hero.ability.Ability;
import de.lemonpvp.helden.item.CustomItem;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Heldenauswahl - funktioniert auf Java wie auf Bedrock. */
public final class HeroMenu extends Menu {

    private static final int[] SMALL_LAYOUT = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] LARGE_LAYOUT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};

    private final List<Hero> heroes;

    public HeroMenu(HeldenPlugin plugin) {
        super(plugin);
        this.heroes = plugin.heroes().sorted();
    }

    @Override
    public String title() {
        return plugin.messages().raw("hero.menu-title");
    }

    @Override
    public int size() {
        return heroes.size() <= SMALL_LAYOUT.length ? 27 : 45;
    }

    private int[] layout() {
        return heroes.size() <= SMALL_LAYOUT.length ? SMALL_LAYOUT : LARGE_LAYOUT;
    }

    @Override
    protected void build(Player viewer) {
        HeldenProfile profile = plugin.profiles().getOrCreate(viewer);
        boolean changeAllowed = changeAllowed(viewer, profile);
        int[] layout = layout();

        for (int index = 0; index < heroes.size() && index < layout.length; index++) {
            Hero hero = heroes.get(index);
            boolean current = hero.id().equalsIgnoreCase(profile.heroId());
            set(layout[index], icon(hero.icon(), hero.display(),
                    describe(viewer, hero, current, changeAllowed), "hero:" + hero.id()));
        }
    }

    private boolean changeAllowed(Player viewer, HeldenProfile profile) {
        return plugin.heroes().canChangeNow(viewer, profile);
    }

    private List<String> describe(Player viewer, Hero hero, boolean current, boolean changeAllowed) {
        List<String> lore = new ArrayList<>(hero.description());
        lore.add("");

        Ability ability = plugin.abilities().get(hero.abilityId());
        if (ability != null) {
            lore.add("&8» &7Faehigkeit: " + ability.display());
            lore.add("&8» &7Abklingzeit: &f" + ability.cooldownSeconds() + "s");
        }
        CustomItem weapon = plugin.items().get(hero.weaponId());
        if (weapon != null) {
            lore.add("&8» &7Waffe: " + weapon.display());
        }
        lore.add("");

        if (current) {
            lore.add("&a✔ &7Dein aktueller Held");
        } else if (changeAllowed) {
            lore.add(plugin.messages().raw("hero.select-lore"));
        } else {
            HeldenProfile profile = plugin.profiles().getOrCreate(viewer);
            long remaining = plugin.heroes().changeRemaining(profile);
            lore.add(plugin.messages().raw("hero.locked-lore")
                    + (remaining > 0 ? " &8(" + TimeUtil.format(remaining) + ")" : ""));
        }
        return lore;
    }

    @Override
    public void onClick(Player viewer, int slot, ItemStack clicked, String action) {
        if (action == null || !action.startsWith("hero:")) {
            return;
        }
        Hero hero = plugin.heroes().get(action.substring("hero:".length()));
        if (hero == null) {
            return;
        }
        viewer.closeInventory();
        plugin.heroes().trySelect(viewer, hero);
    }
}

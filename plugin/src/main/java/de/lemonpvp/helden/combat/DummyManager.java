package de.lemonpvp.helden.combat;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Keys;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Der Combat-Log-Dummy.
 *
 * <p>Wer sich im Kampf ausloggt, verliert nicht sofort ein Herz. Stattdessen
 * bleibt eine Puppe mit seiner kompletten Ausruestung stehen. Wird die
 * getoetet, sind Herz und Loot weg; ueberlebt sie, kommt der Spieler mit einem
 * blauen Auge davon.</p>
 */
public final class DummyManager {

    /** Was zu einer aufgestellten Puppe gehoert. */
    private record Dummy(UUID owner, String ownerName, List<ItemStack> loot, long expiresAt) {
    }

    private final HeldenPlugin plugin;
    private final Map<UUID, Dummy> dummies = new HashMap<>();
    private BukkitTask task;

    public DummyManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public int size() {
        return dummies.size();
    }

    /**
     * Stellt die Puppe auf und nimmt dem Spieler dabei sein Inventar ab - es
     * haengt ab jetzt an der Puppe.
     */
    public void spawnFor(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        List<ItemStack> loot = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && !stack.getType().isAir()) {
                loot.add(stack.clone());
            }
        }

        Zombie dummy = world.spawn(location, Zombie.class);
        dummy.setAdult();
        dummy.setAI(false);
        dummy.setSilent(true);
        dummy.setCanPickupItems(false);
        dummy.setRemoveWhenFarAway(false);
        dummy.setCustomName(Text.color(Text.replace(plugin.settings().dummyNameFormat(),
                "%player%", player.getName())));
        dummy.setCustomNameVisible(true);
        dummy.getPersistentDataContainer().set(Keys.dummyOwner(), PersistentDataType.STRING,
                player.getUniqueId().toString());

        dressUp(dummy, player);

        // Das Inventar wandert komplett an die Puppe.
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        dummies.put(dummy.getUniqueId(), new Dummy(
                player.getUniqueId(),
                player.getName(),
                loot,
                System.currentTimeMillis() + plugin.settings().dummyLifetimeSeconds() * 1000L));

        plugin.messages().broadcastRaw("dummy.spawned", "%player%", player.getName());
    }

    /** Zieht der Puppe die Sachen des Spielers an - sichtbar fuer alle. */
    private void dressUp(Zombie dummy, Player player) {
        EntityEquipment equipment = dummy.getEquipment();
        if (equipment == null) {
            return;
        }
        equipment.setArmorContents(player.getInventory().getArmorContents());
        equipment.setItemInMainHand(player.getInventory().getItemInMainHand());
        equipment.setItemInOffHand(player.getInventory().getItemInOffHand());

        // Die Ausruestung selbst darf nicht droppen - wir werfen beim Tod das
        // gespeicherte Inventar aus, sonst gaebe es alles doppelt.
        equipment.setHelmetDropChance(0.0f);
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);
        equipment.setItemInMainHandDropChance(0.0f);
        equipment.setItemInOffHandDropChance(0.0f);
    }

    public boolean isDummy(Entity entity) {
        return entity != null && dummies.containsKey(entity.getUniqueId());
    }

    /**
     * Die Puppe wurde erledigt: der Besitzer verliert Herz und Loot.
     *
     * @return die Gegenstaende, die stattdessen fallen sollen
     */
    public List<ItemStack> handleDeath(Entity entity, Player killer) {
        Dummy dummy = dummies.remove(entity.getUniqueId());
        if (dummy == null) {
            return List.of();
        }

        plugin.messages().broadcastRaw("dummy.killed",
                "%player%", dummy.ownerName(),
                "%killer%", killer == null ? "?" : killer.getName());

        if (killer != null) {
            HeldenProfile killerProfile = plugin.profiles().getOrCreate(killer);
            killerProfile.addKill();
            Compat.sound(killer, "entity.player.levelup", 0.8f, 1.6f);
        }

        HeldenProfile owner = plugin.profiles().get(dummy.owner());
        if (owner != null) {
            owner.addPvpDeath();
            plugin.hearts().loseHeart(owner, killer == null ? null : killer.getName(), new HashSet<>());
        }
        return dummy.loot();
    }

    /**
     * Der Spieler ist rechtzeitig zurueck - Puppe weg, alles bleibt ihm.
     *
     * @return {@code true}, wenn eine Puppe eingesammelt wurde
     */
    public boolean rescue(Player player) {
        UUID entityId = entityIdOf(player.getUniqueId());
        if (entityId == null) {
            return false;
        }
        Dummy dummy = dummies.remove(entityId);
        removeEntity(entityId);
        if (dummy == null) {
            return false;
        }

        for (ItemStack stack : dummy.loot()) {
            player.getInventory().addItem(stack);
        }
        plugin.messages().send(player, "dummy.rescued");
        return true;
    }

    /** Restsekunden der eigenen Puppe, 0 wenn keine steht. */
    public long remainingSecondsFor(UUID owner) {
        UUID entityId = entityIdOf(owner);
        if (entityId == null) {
            return 0L;
        }
        Dummy dummy = dummies.get(entityId);
        if (dummy == null) {
            return 0L;
        }
        return Math.max(0L, (dummy.expiresAt() - System.currentTimeMillis()) / 1000L);
    }

    private UUID entityIdOf(UUID owner) {
        for (Map.Entry<UUID, Dummy> entry : dummies.entrySet()) {
            if (entry.getValue().owner().equals(owner)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, Dummy> entry : dummies.entrySet()) {
            Entity entity = plugin.getServer().getEntity(entry.getKey());
            if (entity == null || entity.isDead()) {
                expired.add(entry.getKey());
                continue;
            }
            // Puppen sollen nicht in der Sonne verbrennen.
            entity.setFireTicks(0);
            if (now >= entry.getValue().expiresAt()) {
                expired.add(entry.getKey());
                plugin.messages().broadcastRaw("dummy.expired", "%player%", entry.getValue().ownerName());
            }
        }

        for (UUID entityId : expired) {
            dummies.remove(entityId);
            removeEntity(entityId);
        }
    }

    private void removeEntity(UUID entityId) {
        Entity entity = plugin.getServer().getEntity(entityId);
        if (entity != null) {
            entity.remove();
        }
    }

    /** Entfernt alle Puppen - beim Reload und beim Herunterfahren. */
    public int removeAll() {
        int amount = dummies.size();
        for (UUID entityId : new ArrayList<>(dummies.keySet())) {
            removeEntity(entityId);
        }
        dummies.clear();
        return amount;
    }

    /**
     * Raeumt Puppen weg, die einen Serverabsturz ueberlebt haben. Sie sind ueber
     * den PersistentDataContainer markiert und daher sicher erkennbar.
     */
    public int removeOrphans() {
        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (dummies.containsKey(entity.getUniqueId())) {
                    continue;
                }
                String owner = entity.getPersistentDataContainer()
                        .get(Keys.dummyOwner(), PersistentDataType.STRING);
                if (owner != null) {
                    entity.remove();
                    removed++;
                }
            }
        }
        return removed;
    }
}

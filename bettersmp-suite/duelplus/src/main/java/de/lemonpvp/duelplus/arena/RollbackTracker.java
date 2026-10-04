package de.lemonpvp.duelplus.arena;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rollt jede Block-Aenderung in einer laufenden Arena zurueck, sobald
 * das Duell vorbei ist - Bauen/Abbauen ist waehrend des Kampfes erlaubt
 * (anders als z.B. bei LobbyLock), aber danach soll die Arena wieder
 * genau so aussehen wie vorher, ohne sie neu erzeugen zu muessen.
 *
 * Merkt sich je Block NUR den allerersten gesehenen Zustand einer
 * laufenden Runde (spaetere Aenderungen an derselben Stelle
 * ueberschreiben den Merkposten nicht) - beim Zuruecksetzen wird dann
 * einfach jeder gemerkte Block auf genau diesen Ursprungszustand gesetzt.
 *
 * Deckt Abbauen/Platzieren/Explosionen/Eimer/Fluessigkeitsfluss/Feuer
 * ab - das faengt praktisch jede realistische Aenderung waehrend eines
 * kurzen Duells ab. Bewusst NICHT abgedeckt: fallende Bloecke (Sand/
 * Kies) durch Schwerkraft, weil das eher kosmetisch ist und das ohnehin
 * schon breite Abdeckung noch komplexer gemacht haette.
 */
public final class RollbackTracker implements Listener {

    private record Koordinate(String welt, int x, int y, int z) {
        static Koordinate von(Block block) {
            return new Koordinate(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }

    private final Set<String> verfolgteWelten = ConcurrentHashMap.newKeySet();
    private final Map<Koordinate, BlockState> ursprung = new ConcurrentHashMap<>();

    public void verfolgungStarten(World welt) {
        verfolgteWelten.add(welt.getName());
    }

    /** Setzt alle gemerkten Bloecke dieser Welt zurueck und vergisst sie danach wieder. */
    public void zuruecksetzenUndStoppen(World welt) {
        verfolgteWelten.remove(welt.getName());
        Map<Koordinate, BlockState> betroffene = new LinkedHashMap<>();
        ursprung.entrySet().removeIf(entry -> {
            if (entry.getKey().welt().equals(welt.getName())) {
                betroffene.put(entry.getKey(), entry.getValue());
                return true;
            }
            return false;
        });
        for (BlockState zustand : betroffene.values()) {
            zustand.update(true, false);
        }
    }

    private boolean istVerfolgt(World welt) {
        return welt != null && verfolgteWelten.contains(welt.getName());
    }

    private void merken(BlockState zustand) {
        if (!istVerfolgt(zustand.getWorld())) {
            return;
        }
        ursprung.putIfAbsent(Koordinate.von(zustand.getBlock()), zustand);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimAbbauen(BlockBreakEvent event) {
        merken(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimPlatzieren(BlockPlaceEvent event) {
        merken(event.getBlockReplacedState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beiEntityExplosion(EntityExplodeEvent event) {
        if (!istVerfolgt(event.getLocation().getWorld())) {
            return;
        }
        for (Block block : event.blockList()) {
            merken(block.getState());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beiBlockExplosion(BlockExplodeEvent event) {
        if (!istVerfolgt(event.getBlock().getWorld())) {
            return;
        }
        for (Block block : event.blockList()) {
            merken(block.getState());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimEimerLeeren(PlayerBucketEmptyEvent event) {
        merken(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimEimerFuellen(PlayerBucketFillEvent event) {
        merken(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beiFluessigkeitsfluss(BlockFromToEvent event) {
        merken(event.getToBlock().getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimVerbrennen(BlockBurnEvent event) {
        merken(event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void beimEntzuenden(BlockIgniteEvent event) {
        merken(event.getBlock().getState());
    }
}

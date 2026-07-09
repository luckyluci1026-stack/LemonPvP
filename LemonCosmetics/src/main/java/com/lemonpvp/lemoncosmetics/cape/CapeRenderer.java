package com.lemonpvp.lemoncosmetics.cape;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * Draws the active {@link CapeImage} frame onto a map. The animation ticker
 * calls {@link #setFrame(int)}; the next {@link #render} redraws the shared map
 * buffer (which the server then streams to every tracking client), so animation
 * costs one buffer write per frame change rather than one per tick per viewer.
 */
public class CapeRenderer extends MapRenderer {

    private final CapeImage image;
    private volatile int frame;
    private volatile boolean dirty = true;

    public CapeRenderer(CapeImage image) {
        super(false); // non-contextual: the same buffer for all viewers
        this.image = image;
    }

    public int frameCount() { return image.frameCount(); }

    public void setFrame(int f) {
        int next = Math.floorMod(f, Math.max(1, image.frameCount()));
        if (next != frame) {
            frame = next;
            dirty = true;
        }
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (!dirty) return;
        dirty = false;
        canvas.drawImage(0, 0, image.frame(frame));
    }
}

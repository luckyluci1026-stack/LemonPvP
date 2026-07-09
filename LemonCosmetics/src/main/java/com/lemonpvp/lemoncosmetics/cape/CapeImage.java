package com.lemonpvp.lemoncosmetics.cape;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Decodes an uploaded cosmetic image into 128x128 ARGB frames ready to be drawn
 * onto a Minecraft map canvas — 100% server-side, no resource pack.
 *
 * <ul>
 *   <li>{@code .png} / {@code .jpg} → a single static frame.</li>
 *   <li>{@code .gif} → one frame per GIF image (basic over-compositing), giving
 *       a smooth animated cape when the {@link CapeRenderer} advances frames.</li>
 * </ul>
 *
 * Frames are scaled to the map's 128x128 grid. The uploaded aspect ratio (e.g.
 * 128x64) is corrected visually by the ItemDisplay's non-uniform scale, so the
 * map itself always fills the full square.
 */
public final class CapeImage {

    /** Minecraft map resolution. */
    public static final int SIZE = 128;
    /** Hard cap on animation frames to bound memory and map-packet bandwidth. */
    public static final int MAX_FRAMES = 60;

    private final List<BufferedImage> frames = new ArrayList<>();

    private CapeImage() {}

    public int frameCount() { return frames.size(); }

    public BufferedImage frame(int index) {
        return frames.get(Math.floorMod(index, frames.size()));
    }

    /** Loads and normalizes an image file. Throws if it can't be decoded. */
    public static CapeImage load(File file) throws Exception {
        CapeImage img = new CapeImage();
        String name = file.getName().toLowerCase();
        if (name.endsWith(".gif")) {
            img.loadGif(file);
        } else {
            BufferedImage src = ImageIO.read(file);
            if (src == null) throw new IllegalArgumentException("Unsupported/unreadable image: " + file.getName());
            img.frames.add(scale(src));
        }
        if (img.frames.isEmpty()) throw new IllegalArgumentException("No frames decoded from " + file.getName());
        return img;
    }

    private void loadGif(File file) throws Exception {
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) throw new IllegalStateException("No GIF reader available");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, false);
                int count = reader.getNumImages(true);
                BufferedImage canvas = null;
                for (int i = 0; i < count && frames.size() < MAX_FRAMES; i++) {
                    BufferedImage frame = reader.read(i);
                    if (canvas == null) {
                        canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    }
                    // Basic over-compositing: draw this frame onto the running canvas
                    // so partial/additive GIF frames still render fully.
                    Graphics2D g = canvas.createGraphics();
                    g.drawImage(frame, 0, 0, null);
                    g.dispose();
                    frames.add(scale(deepCopy(canvas)));
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static BufferedImage scale(BufferedImage src) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return out;
    }
}

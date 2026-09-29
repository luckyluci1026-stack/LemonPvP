package de.lemonpvp.duelplus.replay;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import static de.lemonpvp.duelplus.replay.ReplayDaten.*;

public final class ReplaySchreiber {

    private static final int MAX_TEXT = 256;

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 * 1024);
    private final GZIPOutputStream gzip;
    private final DataOutputStream aus;
    private final Map<String, Integer> palette = new HashMap<>();
    private int bilder;
    private boolean fertig;

    public ReplaySchreiber(Kopf kopf) {
        try {
            gzip = new GZIPOutputStream(bytes, 8192) {
                {
                    def.setLevel(Deflater.BEST_COMPRESSION);
                }
            };
            aus = new DataOutputStream(gzip);
            aus.writeInt(MAGIC);
            aus.writeShort(VERSION);
            aus.writeUTF(kopf.duellId());
            aus.writeUTF(kopf.arena());
            aus.writeDouble(kopf.mitteX());
            aus.writeDouble(kopf.mitteY());
            aus.writeDouble(kopf.mitteZ());
            aus.writeLong(kopf.start());
            aus.writeByte(kopf.spieler().size());
            for (Teilnehmer t : kopf.spieler()) {
                aus.writeLong(t.id().getMostSignificantBits());
                aus.writeLong(t.id().getLeastSignificantBits());
                aus.writeUTF(t.name());
                aus.writeUTF(t.skinWert() == null ? "" : t.skinWert());
                aus.writeUTF(t.skinSignatur() == null ? "" : t.skinSignatur());
            }
        } catch (IOException fehler) {
            throw new UncheckedIOException(fehler);
        }
    }

    private interface Schritt {
        void schreiben() throws IOException;
    }

    private void los(Schritt schritt) {
        if (fertig) {
            return;
        }
        try {
            schritt.schreiben();
        } catch (IOException fehler) {
            throw new UncheckedIOException(fehler);
        }
    }

    private int paletteId(String text) throws IOException {
        Integer id = palette.get(text);
        if (id != null) {
            return id;
        }
        int neu = palette.size();
        palette.put(text, neu);
        aus.writeByte(PALETTE);
        varInt(aus, neu);
        aus.writeUTF(text);
        return neu;
    }

    public void bild(Zustand[] spieler) {
        los(() -> {
            aus.writeByte(BILD);
            for (Zustand z : spieler) {
                if (z == null || !z.anwesend()) {
                    aus.writeByte(0);
                    continue;
                }
                aus.writeByte(z.flags() | ANWESEND);
                aus.writeFloat(z.x());
                aus.writeFloat(z.y());
                aus.writeFloat(z.z());
                aus.writeByte(winkel(z.yaw()));
                aus.writeByte(winkel(z.pitch()));
                aus.writeByte(Math.max(0, Math.min(255, Math.round(z.leben() * 2))));
            }
            bilder++;
        });
    }

    public void schwung(int spieler, boolean nebenhand) {
        los(() -> {
            aus.writeByte(SCHWUNG);
            aus.writeByte(spieler);
            aus.writeBoolean(nebenhand);
        });
    }

    public void schaden(int spieler, float menge) {
        los(() -> {
            aus.writeByte(SCHADEN);
            aus.writeByte(spieler);
            aus.writeFloat(menge);
        });
    }

    public void ausruestung(int spieler, int slot, byte[] item) {
        los(() -> {
            aus.writeByte(AUSRUESTUNG);
            aus.writeByte(spieler);
            aus.writeByte(slot);
            byte[] daten = item == null ? new byte[0] : item;
            varInt(aus, daten.length);
            aus.write(daten);
        });
    }

    public void block(int x, int y, int z, String daten) {
        los(() -> {
            int id = paletteId(daten);
            aus.writeByte(BLOCK);
            aus.writeShort(x);
            aus.writeShort(y);
            aus.writeShort(z);
            varInt(aus, id);
        });
    }

    public void entityNeu(int id, String material) {
        los(() -> {
            int eintrag = paletteId(material);
            aus.writeByte(ENTITY_NEU);
            aus.writeInt(id);
            varInt(aus, eintrag);
        });
    }

    public void entityPos(int id, float x, float y, float z, float yaw, float pitch) {
        los(() -> {
            aus.writeByte(ENTITY_POS);
            aus.writeInt(id);
            aus.writeFloat(x);
            aus.writeFloat(y);
            aus.writeFloat(z);
            aus.writeByte(winkel(yaw));
            aus.writeByte(winkel(pitch));
        });
    }

    public void entityWeg(int id) {
        los(() -> {
            aus.writeByte(ENTITY_WEG);
            aus.writeInt(id);
        });
    }

    public void chat(int spieler, String text) {
        los(() -> {
            aus.writeByte(CHAT);
            aus.writeByte(spieler);
            aus.writeUTF(text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text);
        });
    }

    public void explosion(float x, float y, float z) {
        los(() -> {
            aus.writeByte(EXPLOSION);
            aus.writeFloat(x);
            aus.writeFloat(y);
            aus.writeFloat(z);
        });
    }

    public void totem(int spieler) {
        los(() -> {
            aus.writeByte(TOTEM);
            aus.writeByte(spieler);
        });
    }

    public void grenze(float groesse) {
        los(() -> {
            aus.writeByte(GRENZE);
            aus.writeFloat(groesse);
        });
    }

    public int bilder() {
        return bilder;
    }

    public int ungefaehreGroesse() {
        return bytes.size();
    }

    public byte[] beenden(int ergebnis, int gewinner) {
        if (!fertig) {
            try {
                aus.writeByte(ENDE);
                aus.writeByte(ergebnis);
                aus.writeByte(gewinner);
                aus.writeInt(bilder);
                aus.flush();
                gzip.finish();
            } catch (IOException fehler) {
                throw new UncheckedIOException(fehler);
            }
            fertig = true;
        }
        return bytes.toByteArray();
    }
}

package de.lemonpvp.duelplus.replay;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public final class ReplayDaten {

    public static final int MAGIC = 0x42525031;
    public static final short VERSION = 1;

    public static final byte BILD = 1;
    public static final byte SCHWUNG = 2;
    public static final byte SCHADEN = 3;
    public static final byte AUSRUESTUNG = 4;
    public static final byte PALETTE = 5;
    public static final byte BLOCK = 6;
    public static final byte ENTITY_NEU = 7;
    public static final byte ENTITY_POS = 8;
    public static final byte ENTITY_WEG = 9;
    public static final byte CHAT = 10;
    public static final byte EXPLOSION = 11;
    public static final byte TOTEM = 12;
    public static final byte GRENZE = 13;
    public static final byte ENDE = 99;

    public static final int ANWESEND = 1;
    public static final int SCHLEICHEN = 2;
    public static final int SPRINTEN = 4;
    public static final int SCHWIMMEN = 8;
    public static final int BLOCKEN = 16;
    public static final int BRENNT = 32;
    public static final int UNSICHTBAR = 64;
    public static final int GLEITEN = 128;

    public static final int ERGEBNIS_SIEG = 0;
    public static final int ERGEBNIS_UNENTSCHIEDEN = 1;
    public static final int ERGEBNIS_AUFGABE = 2;
    public static final int ERGEBNIS_ABGEBROCHEN = 3;

    public static final int SLOT_HAND = 0;
    public static final int SLOT_NEBENHAND = 1;
    public static final int SLOT_KOPF = 2;
    public static final int SLOT_BRUST = 3;
    public static final int SLOT_BEINE = 4;
    public static final int SLOT_FUESSE = 5;
    public static final int SLOTS = 6;

    private ReplayDaten() {
    }

    public record Teilnehmer(UUID id, String name, String skinWert, String skinSignatur) {
    }

    public record Kopf(String duellId, String arena, double mitteX, double mitteY, double mitteZ, long start,
                       List<Teilnehmer> spieler) {
    }

    public record Zustand(boolean anwesend, float x, float y, float z, float yaw, float pitch, int flags, float leben) {

        public static final Zustand WEG = new Zustand(false, 0, 0, 0, 0, 0, 0, 0);

        public boolean hat(int flag) {
            return (flags & flag) != 0;
        }
    }

    public sealed interface Ereignis permits Schwung, Schaden, Ausruestung, Block, EntityNeu, EntityPos, EntityWeg,
            Chat, Explosion, Totem, Grenze {
    }

    public record Schwung(int spieler, boolean nebenhand) implements Ereignis {
    }

    public record Schaden(int spieler, float menge) implements Ereignis {
    }

    public record Ausruestung(int spieler, int slot, byte[] item) implements Ereignis {
    }

    public record Block(int x, int y, int z, String daten) implements Ereignis {
    }

    public record EntityNeu(int id, String material) implements Ereignis {
    }

    public record EntityPos(int id, float x, float y, float z, float yaw, float pitch) implements Ereignis {
    }

    public record EntityWeg(int id) implements Ereignis {
    }

    public record Chat(int spieler, String text) implements Ereignis {
    }

    public record Explosion(float x, float y, float z) implements Ereignis {
    }

    public record Totem(int spieler) implements Ereignis {
    }

    public record Grenze(float groesse) implements Ereignis {
    }

    public record Bild(Zustand[] spieler, List<Ereignis> ereignisse) {
    }

    public record Ende(int ergebnis, int gewinner, int ticks) {
    }

    public record Replay(Kopf kopf, List<Bild> bilder, Ende ende) {
    }

    public static float winkel(byte roh) {
        return roh * 360f / 256f;
    }

    public static byte winkel(float grad) {
        return (byte) Math.round((((grad % 360f) + 360f) % 360f) * 256f / 360f);
    }

    public static void varInt(DataOutput aus, int wert) throws IOException {
        int rest = wert;
        while ((rest & ~0x7F) != 0) {
            aus.writeByte((rest & 0x7F) | 0x80);
            rest >>>= 7;
        }
        aus.writeByte(rest);
    }

    public static int varInt(DataInput ein) throws IOException {
        int wert = 0;
        int verschiebung = 0;
        while (true) {
            int b = ein.readUnsignedByte();
            wert |= (b & 0x7F) << verschiebung;
            if ((b & 0x80) == 0) {
                return wert;
            }
            verschiebung += 7;
            if (verschiebung > 28) {
                throw new IOException("VarInt zu lang");
            }
        }
    }

    public static Kopf kopfLesen(DataInput ein) throws IOException {
        if (ein.readInt() != MAGIC) {
            throw new IOException("Keine Replay-Datei");
        }
        short version = ein.readShort();
        if (version != VERSION) {
            throw new IOException("Unbekannte Replay-Version " + version);
        }
        String duellId = ein.readUTF();
        String arena = ein.readUTF();
        double x = ein.readDouble();
        double y = ein.readDouble();
        double z = ein.readDouble();
        long start = ein.readLong();
        int anzahl = ein.readUnsignedByte();
        List<Teilnehmer> spieler = new ArrayList<>();
        for (int i = 0; i < anzahl; i++) {
            spieler.add(new Teilnehmer(new UUID(ein.readLong(), ein.readLong()), ein.readUTF(), ein.readUTF(), ein.readUTF()));
        }
        return new Kopf(duellId, arena, x, y, z, start, List.copyOf(spieler));
    }

    public static Replay lesen(InputStream roh) throws IOException {
        DataInputStream ein = new DataInputStream(new GZIPInputStream(roh, 65_536));
        Kopf kopf = kopfLesen(ein);
        int anzahl = kopf.spieler().size();
        Map<Integer, String> palette = new HashMap<>();
        List<Bild> bilder = new ArrayList<>();
        List<Ereignis> aktuell = null;
        Ende ende = null;
        try {
            while (ende == null) {
                byte typ = ein.readByte();
                switch (typ) {
                    case BILD -> {
                        Zustand[] zustaende = new Zustand[anzahl];
                        for (int i = 0; i < anzahl; i++) {
                            int flags = ein.readUnsignedByte();
                            if ((flags & ANWESEND) == 0) {
                                zustaende[i] = Zustand.WEG;
                                continue;
                            }
                            zustaende[i] = new Zustand(true, ein.readFloat(), ein.readFloat(), ein.readFloat(),
                                    winkel(ein.readByte()), winkel(ein.readByte()), flags, ein.readUnsignedByte() / 2f);
                        }
                        aktuell = new ArrayList<>();
                        bilder.add(new Bild(zustaende, aktuell));
                    }
                    case PALETTE -> palette.put(varInt(ein), ein.readUTF());
                    case ENDE -> ende = new Ende(ein.readUnsignedByte(), ein.readByte(), ein.readInt());
                    default -> {
                        Ereignis ereignis = ereignisLesen(typ, ein, palette);
                        if (aktuell != null && ereignis != null) {
                            aktuell.add(ereignis);
                        }
                    }
                }
            }
        } catch (EOFException abgeschnitten) {
            ende = new Ende(ERGEBNIS_ABGEBROCHEN, -1, bilder.size());
        }
        return new Replay(kopf, bilder, ende);
    }

    private static Ereignis ereignisLesen(byte typ, DataInput ein, Map<Integer, String> palette) throws IOException {
        return switch (typ) {
            case SCHWUNG -> new Schwung(ein.readUnsignedByte(), ein.readBoolean());
            case SCHADEN -> new Schaden(ein.readUnsignedByte(), ein.readFloat());
            case AUSRUESTUNG -> {
                int spieler = ein.readUnsignedByte();
                int slot = ein.readUnsignedByte();
                int laenge = varInt(ein);
                if (laenge > 1 << 20) {
                    throw new IOException("Gegenstand zu gross");
                }
                byte[] item = new byte[laenge];
                ein.readFully(item);
                yield new Ausruestung(spieler, slot, item);
            }
            case BLOCK -> new Block(ein.readShort(), ein.readShort(), ein.readShort(), palette.getOrDefault(varInt(ein), "minecraft:air"));
            case ENTITY_NEU -> new EntityNeu(ein.readInt(), palette.getOrDefault(varInt(ein), "BARRIER"));
            case ENTITY_POS -> new EntityPos(ein.readInt(), ein.readFloat(), ein.readFloat(), ein.readFloat(),
                    winkel(ein.readByte()), winkel(ein.readByte()));
            case ENTITY_WEG -> new EntityWeg(ein.readInt());
            case CHAT -> new Chat(ein.readUnsignedByte(), ein.readUTF());
            case EXPLOSION -> new Explosion(ein.readFloat(), ein.readFloat(), ein.readFloat());
            case TOTEM -> new Totem(ein.readUnsignedByte());
            case GRENZE -> new Grenze(ein.readFloat());
            default -> throw new IOException("Unbekannter Eintrag " + typ);
        };
    }
}

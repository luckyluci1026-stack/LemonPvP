package de.lemonpvp.dbwipe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimaler, selbst geschriebener TAR-Schreiber (USTAR-Format) - bewusst
 * ohne zusaetzliche Bibliothek, das Format besteht nur aus einfachen
 * 512-Byte-Bloecken. Die exakte Byte-Aufteilung wurde vor dem Einbau
 * gegen Pythons tarfile-Modul geprueft (Rundreise-Test: Datei schreiben,
 * mit tarfile wieder auslesen, Inhalt byteweise vergleichen) - wichtig,
 * weil dieses Backup der EINZIGE Rueckweg ist, bevor DBWipe die
 * Datenbank leert.
 */
final class TarWriter implements AutoCloseable {

    private final OutputStream out;
    private boolean geschlossen = false;

    TarWriter(OutputStream out) {
        this.out = out;
    }

    void schreibeDatei(String name, byte[] inhalt) throws IOException {
        byte[] header = new byte[512];
        schreibeFeld(header, 0, 100, name);
        schreibeOktal(header, 100, 8, 0100644); // Dateirechte rw-r--r--
        schreibeOktal(header, 108, 8, 0);
        schreibeOktal(header, 116, 8, 0);
        schreibeOktal(header, 124, 12, inhalt.length);
        schreibeOktal(header, 136, 12, System.currentTimeMillis() / 1000L);
        for (int i = 148; i < 156; i++) {
            header[i] = ' ';
        }
        header[156] = '0'; // Typeflag: normale Datei
        schreibeFeld(header, 257, 6, "ustar");
        header[263] = '0';
        header[264] = '0';

        long summe = 0;
        for (byte b : header) {
            summe += (b & 0xFF);
        }
        String checksumOktal = zeroPad(Long.toOctalString(summe), 6);
        schreibeFeld(header, 148, 6, checksumOktal);
        header[154] = 0;
        header[155] = ' ';

        out.write(header);
        out.write(inhalt);
        int rest = (int) (-((long) inhalt.length) & 511L);
        if (rest > 0) {
            out.write(new byte[rest]);
        }
    }

    private void schreibeFeld(byte[] header, int offset, int laenge, String wert) {
        byte[] bytes = wert.getBytes(StandardCharsets.US_ASCII);
        int n = Math.min(bytes.length, laenge);
        System.arraycopy(bytes, 0, header, offset, n);
    }

    /** length-1 Oktal-Stellen, links mit '0' aufgefuellt - das letzte Byte bleibt NUL. */
    private void schreibeOktal(byte[] header, int offset, int laenge, long wert) {
        String oktal = zeroPad(Long.toOctalString(wert), laenge - 1);
        schreibeFeld(header, offset, laenge, oktal);
    }

    private static String zeroPad(String oktal, int stellen) {
        if (oktal.length() > stellen) {
            oktal = oktal.substring(oktal.length() - stellen);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = oktal.length(); i < stellen; i++) {
            sb.append('0');
        }
        return sb.append(oktal).toString();
    }

    @Override
    public void close() throws IOException {
        if (geschlossen) {
            return;
        }
        geschlossen = true;
        // Archiv-Ende: zwei Bloecke aus Nullen.
        out.write(new byte[1024]);
        out.flush();
    }
}

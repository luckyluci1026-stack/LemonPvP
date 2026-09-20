package de.lemonpvp.duelplus.db;

import de.lemonpvp.duelplus.DuelPlus;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Datenbank-Schicht fuer DuelPlus - MariaDB ist hier Pflicht, kein
 * SQLite-Ausweichen wie bei BetterSMP: DuelPlus muss Zustand zwischen
 * mehreren eigenstaendigen Serverprozessen (SMP, Lobby, Duels) teilen,
 * eine lokale Datei koennte das grundsaetzlich nicht.
 *
 * Gleiches Verbindungsmuster wie BetterSMPs eigene Database-Klasse:
 * ein Single-Thread-Executor, damit der Haupt-Thread nie durch
 * Datenbank-IO blockiert wird.
 */
public final class DuelDatabase {

    private final DuelPlus plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DuelPlus-DB");
        t.setDaemon(true);
        return t;
    });

    private Driver driver;
    private String url;
    private Properties props;
    private Connection connection;
    private volatile boolean bereit = false;

    public DuelDatabase(DuelPlus plugin) {
        this.plugin = plugin;
    }

    public boolean bereit() {
        return bereit;
    }

    public void init() {
        try {
            String host = plugin.getConfig().getString("database.host", "127.0.0.1");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String db = plugin.getConfig().getString("database.database", "bettersmp");
            String extra = plugin.getConfig().getString("database.properties", "");
            this.url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                    + (extra == null || extra.isBlank() ? "" : "?" + extra);
            this.props = new Properties();
            props.setProperty("user", plugin.getConfig().getString("database.user", "root"));
            props.setProperty("password", plugin.getConfig().getString("database.password", ""));
            this.driver = (Driver) Class.forName("org.mariadb.jdbc.Driver")
                    .getDeclaredConstructor().newInstance();
            this.connection = driver.connect(url, props);
            if (connection == null) {
                throw new SQLException("Treiber akzeptierte die URL nicht: " + url);
            }
            run(this::createTables).join();
            bereit = true;
            plugin.getLogger().info("DuelPlus: mit MariaDB verbunden.");
        } catch (Exception e) {
            bereit = false;
            plugin.getLogger().severe("DuelPlus: MariaDB-Verbindung fehlgeschlagen (" + e.getMessage()
                    + ") - DuelPlus bleibt auf diesem Server ohne Wirkung, bis die Verbindung klappt.");
        }
    }

    private Connection conn() throws SQLException {
        if (connection == null || !connection.isValid(2)) {
            connection = driver.connect(url, props);
        }
        return connection;
    }

    private void createTables() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS duelplus_presence ("
                + "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), server VARCHAR(48), aktualisiert BIGINT)",
            "CREATE TABLE IF NOT EXISTS duelplus_duelle ("
                + "id VARCHAR(36) PRIMARY KEY, "
                + "spieler_a VARCHAR(36), spieler_a_name VARCHAR(32), spieler_a_server VARCHAR(48), "
                + "spieler_b VARCHAR(36), spieler_b_name VARCHAR(32), spieler_b_server VARCHAR(48), "
                + "status VARCHAR(20), arena_welt VARCHAR(64), gewinner VARCHAR(36), erstellt BIGINT, "
                + "a_bearbeitet BOOLEAN DEFAULT FALSE, b_bearbeitet BOOLEAN DEFAULT FALSE, "
                + "ziel_gezeigt BOOLEAN DEFAULT FALSE)",
            "CREATE TABLE IF NOT EXISTS duelplus_inventar ("
                + "duell_id VARCHAR(36), spieler VARCHAR(36), richtung VARCHAR(16), "
                + "daten LONGTEXT, erstellt BIGINT, PRIMARY KEY (duell_id, spieler, richtung))",
            "CREATE TABLE IF NOT EXISTS duelplus_stamm_inventar ("
                + "uuid VARCHAR(36) PRIMARY KEY, daten LONGTEXT, aktualisiert BIGINT)"
        };
        try (var st = conn().createStatement()) {
            for (String sql : ddl) {
                st.executeUpdate(sql);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("DuelPlus: Tabellen konnten nicht angelegt werden: " + e.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // egal beim Herunterfahren
        }
    }

    // ---------------- Ausfuehrungs-Helfer ----------------

    private CompletableFuture<Void> run(Runnable action) {
        return CompletableFuture.runAsync(action, executor);
    }

    private <T> CompletableFuture<T> supply(Supplier<T> action) {
        return CompletableFuture.supplyAsync(action, executor);
    }

    private void warn(String where, SQLException e) {
        plugin.getLogger().warning("DuelPlus DB-Fehler (" + where + "): " + e.getMessage());
    }

    // ================================================================
    //  Anwesenheit (welcher Server, fuer serveruebergreifende Anfragen)
    // ================================================================

    public CompletableFuture<Void> presenceSetzen(UUID uuid, String name, String server) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "REPLACE INTO duelplus_presence(uuid,name,server,aktualisiert) VALUES(?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, server);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("presenceSetzen", e);
            }
        });
    }

    public CompletableFuture<Void> presenceEntfernen(UUID uuid) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement("DELETE FROM duelplus_presence WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("presenceEntfernen", e);
            }
        });
    }

    /** Server, auf dem der Spieler gerade ist - leer, wenn nirgends bekannt oder zu alt (siehe veraltetSekunden). */
    public CompletableFuture<Optional<String>> presenceServerVon(UUID uuid, int veraltetSekunden) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT server, aktualisiert FROM duelplus_presence WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long alterMillis = System.currentTimeMillis() - rs.getLong("aktualisiert");
                        if (alterMillis <= veraltetSekunden * 1000L) {
                            return Optional.of(rs.getString("server"));
                        }
                    }
                }
            } catch (SQLException e) {
                warn("presenceServerVon", e);
            }
            return Optional.empty();
        });
    }

    /** UUID zu einem Namen - fuer /duel <Name>. Nur unter denen, die gerade als anwesend gelten. */
    public CompletableFuture<Optional<UUID>> presenceUuidFuerName(String name, int veraltetSekunden) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT uuid, aktualisiert FROM duelplus_presence WHERE LOWER(name)=LOWER(?)")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long alterMillis = System.currentTimeMillis() - rs.getLong("aktualisiert");
                        if (alterMillis <= veraltetSekunden * 1000L) {
                            return Optional.of(UUID.fromString(rs.getString("uuid")));
                        }
                    }
                }
            } catch (SQLException e) {
                warn("presenceUuidFuerName", e);
            }
            return Optional.empty();
        });
    }

    // ================================================================
    //  Duelle
    // ================================================================

    public CompletableFuture<Void> anfrageErstellen(DuelRecord record) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO duelplus_duelle(id,spieler_a,spieler_a_name,spieler_a_server,"
                            + "spieler_b,spieler_b_name,spieler_b_server,status,erstellt) "
                            + "VALUES(?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, record.id());
                ps.setString(2, record.spielerA().toString());
                ps.setString(3, record.spielerAName());
                ps.setString(4, record.spielerAServer());
                ps.setString(5, record.spielerB().toString());
                ps.setString(6, record.spielerBName());
                ps.setString(7, record.spielerBServer());
                ps.setString(8, record.status());
                ps.setLong(9, record.erstellt());
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("anfrageErstellen", e);
            }
        });
    }

    /** Offene (WARTEND) Anfrage zwischen zwei Spielern, egal in welcher Reihenfolge - fuer Doppel-Anfragen-Schutz. */
    public CompletableFuture<Optional<DuelRecord>> offeneAnfrageZwischen(UUID a, UUID b) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT * FROM duelplus_duelle WHERE status=? AND "
                            + "((spieler_a=? AND spieler_b=?) OR (spieler_a=? AND spieler_b=?))")) {
                ps.setString(1, DuelRecord.WARTEND);
                ps.setString(2, a.toString());
                ps.setString(3, b.toString());
                ps.setString(4, b.toString());
                ps.setString(5, a.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(lese(rs)) : Optional.empty();
                }
            } catch (SQLException e) {
                warn("offeneAnfrageZwischen", e);
                return Optional.empty();
            }
        });
    }

    /** Ist dieser Spieler gerade in IRGENDeiner offenen Anfrage oder einem aktiven Duell verwickelt? */
    public CompletableFuture<Boolean> istBeschaeftigt(UUID spieler) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT 1 FROM duelplus_duelle WHERE (spieler_a=? OR spieler_b=?) "
                            + "AND status IN (?,?,?) LIMIT 1")) {
                ps.setString(1, spieler.toString());
                ps.setString(2, spieler.toString());
                ps.setString(3, DuelRecord.WARTEND);
                ps.setString(4, DuelRecord.ANGENOMMEN);
                ps.setString(5, DuelRecord.AKTIV);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                warn("istBeschaeftigt", e);
                return false;
            }
        });
    }

    /** ANGENOMMEN- oder AKTIV-Duell, an dem dieser Spieler gerade beteiligt ist - fuer die Ankunft auf dem Arena-Server. */
    public CompletableFuture<Optional<DuelRecord>> aktivesDuellFuer(UUID spieler) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT * FROM duelplus_duelle WHERE (spieler_a=? OR spieler_b=?) AND status IN (?,?) LIMIT 1")) {
                ps.setString(1, spieler.toString());
                ps.setString(2, spieler.toString());
                ps.setString(3, DuelRecord.ANGENOMMEN);
                ps.setString(4, DuelRecord.AKTIV);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(lese(rs)) : Optional.empty();
                }
            } catch (SQLException e) {
                warn("aktivesDuellFuer", e);
                return Optional.empty();
            }
        });
    }

    public CompletableFuture<Optional<DuelRecord>> holeById(String id) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM duelplus_duelle WHERE id=?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(lese(rs)) : Optional.empty();
                }
            } catch (SQLException e) {
                warn("holeById", e);
                return Optional.empty();
            }
        });
    }

    /** WARTEND-Anfragen, deren Ziel auf diesem Server ist und dem Ziel noch nicht gezeigt wurde. */
    public CompletableFuture<List<DuelRecord>> offeneFuerZielServer(String server) {
        return supply(() -> liste(
                "SELECT * FROM duelplus_duelle WHERE status=? AND spieler_b_server=? AND ziel_gezeigt=FALSE",
                DuelRecord.WARTEND, server));
    }

    public CompletableFuture<Void> markiereZielGezeigt(String id) {
        return run(() -> update("UPDATE duelplus_duelle SET ziel_gezeigt=TRUE WHERE id=?", id));
    }

    /** Atomarer Statuswechsel, nur wenn der bisherige Status noch passt - true, wenn DIESER Aufruf gewonnen hat. */
    public CompletableFuture<Boolean> statusWechseln(String id, String von, String zu) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE duelplus_duelle SET status=? WHERE id=? AND status=?")) {
                ps.setString(1, zu);
                ps.setString(2, id);
                ps.setString(3, von);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                warn("statusWechseln", e);
                return false;
            }
        });
    }

    /** Alle WARTEND-Zeilen, die aelter als timeoutSekunden sind, auf ABGELAUFEN setzen. */
    public CompletableFuture<Void> verfalleAlte(int timeoutSekunden) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE duelplus_duelle SET status=? WHERE status=? AND erstellt<?")) {
                ps.setString(1, DuelRecord.ABGELAUFEN);
                ps.setString(2, DuelRecord.WARTEND);
                ps.setLong(3, System.currentTimeMillis() - timeoutSekunden * 1000L);
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("verfalleAlte", e);
            }
        });
    }

    /**
     * Sicherheitsnetz: ANGENOMMEN-Duelle, bei denen zu lange niemand auf
     * dem Arena-Server angekommen ist (z.B. eine haengengebliebene
     * BungeeCord-Verbindung) - werden wie abgelaufene Anfragen behandelt,
     * damit sie nicht fuer immer unbearbeitet stehen bleiben.
     */
    public CompletableFuture<Void> verfalleFestsitzendeAngenommen(int sekunden) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE duelplus_duelle SET status=? WHERE status=? AND erstellt<?")) {
                ps.setString(1, DuelRecord.ABGELAUFEN);
                ps.setString(2, DuelRecord.ANGENOMMEN);
                ps.setLong(3, System.currentTimeMillis() - sekunden * 1000L);
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("verfalleFestsitzendeAngenommen", e);
            }
        });
    }

    /** ANGENOMMEN-Duelle mit spieler_a auf diesem Server, die spieler_a-Seite noch nicht bearbeitet hat. */
    public CompletableFuture<List<DuelRecord>> angenommenFuerAServer(String server) {
        return supply(() -> liste(
                "SELECT * FROM duelplus_duelle WHERE status=? AND spieler_a_server=? AND a_bearbeitet=FALSE",
                DuelRecord.ANGENOMMEN, server));
    }

    public CompletableFuture<List<DuelRecord>> angenommenFuerBServer(String server) {
        return supply(() -> liste(
                "SELECT * FROM duelplus_duelle WHERE status=? AND spieler_b_server=? AND b_bearbeitet=FALSE",
                DuelRecord.ANGENOMMEN, server));
    }

    public CompletableFuture<Void> markiereBearbeitetA(String id) {
        return run(() -> update("UPDATE duelplus_duelle SET a_bearbeitet=TRUE WHERE id=?", id));
    }

    public CompletableFuture<Void> markiereBearbeitetB(String id) {
        return run(() -> update("UPDATE duelplus_duelle SET b_bearbeitet=TRUE WHERE id=?", id));
    }

    /** ABGELEHNT/ABGELAUFEN-Duelle, bei denen der Herausforderer (spieler_a) auf diesem Server ist und noch nicht benachrichtigt wurde. */
    public CompletableFuture<List<DuelRecord>> abgeschlossenFuerAServer(String server) {
        return supply(() -> liste(
                "SELECT * FROM duelplus_duelle WHERE status IN (?,?) AND spieler_a_server=? AND a_bearbeitet=FALSE",
                DuelRecord.ABGELEHNT, DuelRecord.ABGELAUFEN, server));
    }

    public CompletableFuture<Void> setzeArenaUndAktiv(String id, String arenaWelt) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE duelplus_duelle SET status=?, arena_welt=? WHERE id=?")) {
                ps.setString(1, DuelRecord.AKTIV);
                ps.setString(2, arenaWelt);
                ps.setString(3, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("setzeArenaUndAktiv", e);
            }
        });
    }

    public CompletableFuture<Void> beenden(String id, UUID gewinner) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE duelplus_duelle SET status=?, gewinner=?, a_bearbeitet=FALSE, b_bearbeitet=FALSE WHERE id=?")) {
                ps.setString(1, DuelRecord.BEENDET);
                ps.setString(2, gewinner.toString());
                ps.setString(3, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("beenden", e);
            }
        });
    }

    /** BEENDET-Duelle mit spieler_a auf diesem Server, noch nicht abgeholt (a_bearbeitet dient hier als "Rueckkehr erledigt"). */
    public CompletableFuture<List<DuelRecord>> beendetFuerAServer(String server) {
        return supply(() -> liste(
                "SELECT * FROM duelplus_duelle WHERE status=? AND spieler_a_server=? AND a_bearbeitet=FALSE",
                DuelRecord.BEENDET, server));
    }

    public CompletableFuture<List<DuelRecord>> beendetFuerBServer(String server) {
        return supply(() -> liste(
                "SELECT * FROM duelplus_duelle WHERE status=? AND spieler_b_server=? AND b_bearbeitet=FALSE",
                DuelRecord.BEENDET, server));
    }

    /** Alte, abgeschlossene Zeilen aufraeumen - reine Haushaltsfuehrung gegen unbegrenztes Wachstum. */
    public CompletableFuture<Void> aufraeumen(int maxAlterStunden) {
        return run(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "DELETE FROM duelplus_duelle WHERE status IN (?,?,?) AND erstellt<?")) {
                ps.setString(1, DuelRecord.ABGELEHNT);
                ps.setString(2, DuelRecord.ABGELAUFEN);
                ps.setString(3, DuelRecord.BEENDET);
                ps.setLong(4, System.currentTimeMillis() - maxAlterStunden * 3_600_000L);
                ps.executeUpdate();
            } catch (SQLException e) {
                warn("aufraeumen", e);
            }
        });
    }

    // ================================================================
    //  Inventar-Transport
    // ================================================================

    public static final String RICHTUNG_HIN = "HIN";
    public static final String RICHTUNG_ZURUECK = "ZURUECK";

    public CompletableFuture<Void> snapshotSchreiben(String duellId, UUID spieler, String richtung, SpielerSnapshot snapshot) {
        return run(() -> {
            try {
                String daten = InventarCodec.kodieren(snapshot);
                try (PreparedStatement ps = conn().prepareStatement(
                        "REPLACE INTO duelplus_inventar(duell_id,spieler,richtung,daten,erstellt) VALUES(?,?,?,?,?)")) {
                    ps.setString(1, duellId);
                    ps.setString(2, spieler.toString());
                    ps.setString(3, richtung);
                    ps.setString(4, daten);
                    ps.setLong(5, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("DuelPlus: Inventar-Snapshot konnte nicht gespeichert werden: " + e.getMessage());
            }
        });
    }

    /** Liest den Snapshot UND loescht ihn gleich - einmal abgeholt, wird er nicht nochmal gebraucht. */
    public CompletableFuture<Optional<SpielerSnapshot>> snapshotHolenUndLoeschen(String duellId, UUID spieler, String richtung) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT daten FROM duelplus_inventar WHERE duell_id=? AND spieler=? AND richtung=?")) {
                ps.setString(1, duellId);
                ps.setString(2, spieler.toString());
                ps.setString(3, richtung);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    SpielerSnapshot snapshot = InventarCodec.dekodieren(rs.getString("daten"));
                    try (PreparedStatement del = conn().prepareStatement(
                            "DELETE FROM duelplus_inventar WHERE duell_id=? AND spieler=? AND richtung=?")) {
                        del.setString(1, duellId);
                        del.setString(2, spieler.toString());
                        del.setString(3, richtung);
                        del.executeUpdate();
                    }
                    return Optional.of(snapshot);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("DuelPlus: Inventar-Snapshot konnte nicht gelesen werden: " + e.getMessage());
                return Optional.empty();
            }
        });
    }

    // ================================================================
    //  Stamm-Inventar (staendig aktueller Spiegel des ECHTEN Inventars,
    //  nur von der Loot-Quelle aus geschrieben - siehe ist-loot-quelle)
    // ================================================================

    /** Wird laufend UEBERSCHRIEBEN (kein Einmal-Transport wie snapshotSchreiben) - ein staendiger Spiegel. */
    public CompletableFuture<Void> stammInventarSchreiben(UUID spieler, SpielerSnapshot snapshot) {
        return run(() -> {
            try {
                String daten = InventarCodec.kodieren(snapshot);
                try (PreparedStatement ps = conn().prepareStatement(
                        "REPLACE INTO duelplus_stamm_inventar(uuid,daten,aktualisiert) VALUES(?,?,?)")) {
                    ps.setString(1, spieler.toString());
                    ps.setString(2, daten);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("DuelPlus: Stamm-Inventar konnte nicht gespeichert werden: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Optional<SpielerSnapshot>> stammInventarLesen(UUID spieler) {
        return supply(() -> {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT daten FROM duelplus_stamm_inventar WHERE uuid=?")) {
                ps.setString(1, spieler.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(InventarCodec.dekodieren(rs.getString("daten")));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("DuelPlus: Stamm-Inventar konnte nicht gelesen werden: " + e.getMessage());
                return Optional.empty();
            }
        });
    }

    // ================================================================
    //  Gemeinsame Helfer
    // ================================================================

    private void update(String sql, String id) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            warn("update " + sql, e);
        }
    }

    private List<DuelRecord> liste(String sql, Object... params) {
        List<DuelRecord> ergebnis = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ergebnis.add(lese(rs));
                }
            }
        } catch (SQLException e) {
            warn("liste " + sql, e);
        }
        return ergebnis;
    }

    private DuelRecord lese(ResultSet rs) throws SQLException {
        String gewinnerRoh = rs.getString("gewinner");
        return new DuelRecord(
                rs.getString("id"),
                UUID.fromString(rs.getString("spieler_a")), rs.getString("spieler_a_name"), rs.getString("spieler_a_server"),
                UUID.fromString(rs.getString("spieler_b")), rs.getString("spieler_b_name"), rs.getString("spieler_b_server"),
                rs.getString("status"),
                rs.getString("arena_welt"),
                gewinnerRoh == null ? null : UUID.fromString(gewinnerRoh),
                rs.getLong("erstellt"),
                rs.getBoolean("a_bearbeitet"),
                rs.getBoolean("b_bearbeitet"),
                rs.getBoolean("ziel_gezeigt"));
    }
}

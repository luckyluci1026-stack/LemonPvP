# LearnDeveloping — Backend

Node.js/Fastify-Server mit PostgreSQL. Macht aus den zuvor simulierten
Funktionen echte: gehashte Passwörter, Sitzungen, TOTP-2FA, E-Mail-Versand,
serverseitige Key-Rotation und ein durchgesetztes Speicherkontingent.

## Schnellstart auf schwacher Hardware (ohne Datenbank-Installation)

Zwei Befehle, keine Konfiguration, kein PostgreSQL:

```bash
cd server
npm install

npm run seed:local -- --email du@example.com --password "deinpasswort"
npm run start:local
```

Dann `http://localhost:3000` öffnen. Die Daten landen in `data/local.db`
(SQLite über das in Node eingebaute `node:sqlite`).

Gemessen auf dieser Testmaschine: **77 MB Arbeitsspeicher**, Start in unter
drei Sekunden, Datenbankdatei anfangs 68 KB. Das läuft auch auf einem alten
Laptop.

> **Hinweis:** SQLite ist für Entwicklung und Einzelplatz gedacht. Für den
> Produktivbetrieb mit mehreren gleichzeitigen Nutzern bleibt PostgreSQL die
> richtige Wahl — der Code ist identisch, nur `DATABASE_URL` unterscheidet sich.

## Produktivbetrieb (PostgreSQL)

```bash
cd server
npm install
cp .env.example .env          # Werte anpassen (mindestens DATABASE_URL + SESSION_SECRET)
npm run migrate               # Tabellen anlegen
node src/seed.js --email admin@deine-domain.de   # ersten Admin anlegen
npm start
```

Der Server liefert standardmäßig auch das Frontend aus (`SERVE_FRONTEND=true`),
sodass `http://localhost:3000` direkt die vollständige App zeigt.

### Datenbank wählen

| `DATABASE_URL` | Verwendet |
|---|---|
| `sqlite:./data/local.db` | SQLite (eingebaut, keine Installation) |
| `sqlite::memory:` | Nur im Arbeitsspeicher (für Tests) |
| `postgres://…` | PostgreSQL |

Beide Varianten bestehen dieselbe Testsuite.

## Datenbank vorbereiten

```bash
sudo -u postgres psql <<'SQL'
CREATE USER learndeveloping WITH PASSWORD 'sicheres-passwort';
CREATE DATABASE learndeveloping OWNER learndeveloping;
SQL
```

Dann in der `.env`:
```
DATABASE_URL=postgres://learndeveloping:sicheres-passwort@localhost:5432/learndeveloping
SESSION_SECRET=<openssl rand -base64 48>
```

## Was der Server übernimmt

| Bereich | Umsetzung |
|---|---|
| Passwörter | scrypt (N=32768, r=8) mit zufälligem Salt, zeitkonstanter Vergleich |
| Sitzungen | Zufallstoken im httpOnly-Cookie; in der Datenbank liegt nur der HMAC-Hash |
| 2FA | Echtes TOTP nach RFC 6238, kompatibel mit Authenticator-Apps, ±1 Zeitfenster |
| E-Mail | Verifizierungscode per SMTP; ohne SMTP wird der Code geloggt statt versendet |
| Botschutz | Turnstile-Token wird bei Cloudflare gegengeprüft |
| KI | Keys bleiben auf dem Server, Rotation über mehrere Keys mit Cooldown |
| Fortschritt | XP serverseitig gedeckelt (max. 500 pro Lektion), Lektionen nur einmal wertbar |
| Speicher | Kontingent wird in einer Transaktion mit `FOR UPDATE` durchgesetzt |
| Ratenbegrenzung | global 300/min, Login 15/10min, Registrierung 10/h, KI 20/min pro Nutzer |

## API

Alle Antworten sind JSON. Authentifizierung über das Sitzungs-Cookie.

### Konten
| Methode | Pfad | Zweck |
|---|---|---|
| POST | `/api/auth/register` | Konto anlegen (startet Sitzung) |
| POST | `/api/auth/login` | Anmelden (`need2fa: true` verlangt `totp`) |
| POST | `/api/auth/logout` | Abmelden |
| GET | `/api/auth/me` | Aktuelles Konto |
| POST | `/api/auth/verify-email` | Code bestätigen |
| POST | `/api/auth/resend-verification` | Neuen Code anfordern |
| POST | `/api/auth/2fa/setup` | TOTP-Geheimnis erzeugen |
| POST | `/api/auth/2fa/enable` | Mit gültigem Code aktivieren |
| POST | `/api/auth/2fa/disable` | Mit Passwort deaktivieren |
| POST | `/api/auth/change-password` | Passwort ändern (beendet andere Sitzungen) |

### Lernen
| Methode | Pfad | Zweck |
|---|---|---|
| PATCH | `/api/profile` | Name, Avatar, aktueller Kurs |
| POST | `/api/progress/complete` | Lektion abschließen (XP + Abzeichen) |
| POST | `/api/progress/xp` | XP für eine richtige Aufgabe |
| GET | `/api/leaderboard` | Rangliste (öffentlich) |
| GET | `/api/teacher/students` | Klassenübersicht (nur Lehrkräfte) |

### IDE
| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/api/projects` | Projekte + Speicherstand |
| PUT | `/api/projects/:id?` | Anlegen oder aktualisieren |
| DELETE | `/api/projects/:id` | Löschen (gibt Speicher frei) |

### KI
| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/api/ai/status` | Ist serverseitig eine KI verfügbar? |
| POST | `/api/ai/assist` | Assistent im Code-Editor |
| GET | `/api/ai/pool` | Key-Zustand (nur Admin) |

Lektionen werden **nicht** über den Server bewertet — das erledigt die lokale
Analyse im Browser. Der Assistent ist der einzige KI-Endpunkt im Normalbetrieb.

### Verwaltung
| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/api/admin/stats` | Kennzahlen inkl. KI-Nutzung |
| GET | `/api/admin/users` | Konten suchen |
| POST | `/api/admin/users` | Konto/Admin anlegen |
| PATCH | `/api/admin/users/:id` | Bearbeiten (Rolle, Sperre) |
| DELETE | `/api/admin/users/:id` | Löschen |
| GET/PATCH/DELETE | `/api/admin/reports/:id?` | Meldungen bearbeiten |

Der letzte aktive Administrator kann weder herabgestuft, gesperrt noch
gelöscht werden — sonst wäre die Verwaltung unerreichbar.

## Betrieb mit systemd

```ini
# /etc/systemd/system/learndeveloping.service
[Unit]
Description=LearnDeveloping
After=network.target postgresql.service

[Service]
Type=simple
User=learndeveloping
WorkingDirectory=/opt/learndeveloping/server
ExecStart=/usr/bin/node src/index.js
Restart=always
RestartSec=5
Environment=NODE_ENV=production
EnvironmentFile=/opt/learndeveloping/server/.env

# Absicherung
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/learndeveloping

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now learndeveloping
sudo journalctl -u learndeveloping -f
```

## Tests

Die Tests laufen gegen einen echten Server samt Datenbank und leeren die
Tabellen zu Beginn — daher **niemals gegen die Produktionsdatenbank starten**.

```bash
# Terminal 1 — mit SQLite, ohne Installation
rm -f data/test.db*
npm run test:migrate
npm run test:server

# Terminal 2
npm test
```

Alle drei Skripte setzen dieselbe Umgebung (`sqlite:./data/test.db`,
`SESSION_SECRET=test`) und lassen sich einzeln überschreiben, etwa
`DATABASE_URL=postgres://… npm test`.

Drei Einstellungen darin sehen nebensächlich aus, sind es aber nicht:

- **`DATABASE_URL` auch für `npm test`.** Der Testprozess greift nicht nur
  über HTTP zu, er legt seinen Administrator direkt in der Datenbank an.
  Ohne die Variable landet der in der voreingestellten PostgreSQL-Adresse
  statt in der des Servers — jeder Test, der ihn braucht, bekommt dann
  eine 401 und der Fehler sieht aus, als läge er im Code.
- **`--test-concurrency=1`.** Mehrere Testdateien leeren dieselben
  Tabellen. Nebeneinander gestartet löscht die eine, was die andere
  gerade angelegt hat.
- **`RATE_LIMIT=false`.** Alle Anfragen kommen von derselben Adresse und
  teilen sich das Kontingent der Anfragenbegrenzung; ab einer gewissen
  Zahl scheitert die Anmeldung mit einem 429 — an der Suite, nicht am
  Code. Im Betrieb bleibt die Begrenzung selbstverständlich an; sie ist
  der Schutz gegen das Ausprobieren von Passwörtern.

Abgedeckt sind unter anderem: Registrierung, doppelte E-Mail, Passwortlänge,
E-Mail-Verifizierung, XP-Deckelung, doppelte Lektionen, Projektkontingent,
2FA-Einrichtung und Login-Pflicht, Rollentrennung, Schutz des letzten Admins,
die Key-Rotation bei Rate-Limits, die Anbieterkette des Profi-Agenten sowie
die Einrichtung von NVIDIA NIM.

Zwei Testdateien brauchen weder Server noch Datenbank und laufen allein:

```bash
node --test test/kette.test.js test/nvidia.test.js
```

## Grenzen

- **Passwort-Zurücksetzen** ist noch nicht implementiert (nur Ändern bei
  bestehender Anmeldung).
- **Backups** sind nicht Teil des Servers — `pg_dump` per Cron einrichten.
- Der Key-Cooldown liegt im Arbeitsspeicher: Bei mehreren Serverprozessen
  hätte jeder seinen eigenen Zustand. Für einen einzelnen Prozess (der
  Normalfall auf einer Maschine) ist das korrekt.

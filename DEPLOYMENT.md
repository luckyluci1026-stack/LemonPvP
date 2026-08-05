# LearnDeveloping — Betrieb & Deployment

Diese Anleitung beschreibt den Betrieb auf eigener Hardware hinter Cloudflare
sowie die Einrichtung der KI-Anbindung.

---

## 1. Schnellstart (lokal testen)

Die App braucht keinen Build-Schritt, aber einen HTTP-Server (nicht `file://`):

```bash
npx serve            # oder: python3 -m http.server
```

Dann `http://localhost:3000` (bzw. `:8000`) öffnen.

**Demo-Zugänge**

| Rolle   | E-Mail          | Passwort     |
|---------|-----------------|--------------|
| Admin   | admin@demo.de   | admin123     |
| Lehrer  | lehrer@demo.de  | lehrer123    |
| Schüler | max@demo.de     | schueler123  |

---

## 2. Betrieb auf eigenem Server

### Referenz-Hardware

Getestet gegen folgendes Profil:

| Komponente | Wert |
|---|---|
| CPU | 4 Kerne, bis 5,7 GHz |
| RAM | 12 GB DDR5 ECC |
| Storage | 150 GB NVMe (RAID 1) + 2–3 TB HDD |

Für die Auslieferung der statischen App ist das massiv überdimensioniert —
die Reserven gehen an die lokale KI (Abschnitt 4).

### Statische Auslieferung mit Caddy

Caddy holt sich das TLS-Zertifikat automatisch:

```caddyfile
learndeveloping.com {
    root * /var/www/learndeveloping
    file_server
    encode gzip zstd

    header {
        X-Content-Type-Options nosniff
        Referrer-Policy strict-origin-when-cross-origin
    }

    # App-Quelle immer frisch ausliefern
    @app path /App.jsx /index.html
    header @app Cache-Control "public, max-age=0, must-revalidate"
}
```

Alternativ nginx:

```nginx
server {
    listen 443 ssl http2;
    server_name learndeveloping.com;
    root /var/www/learndeveloping;

    gzip on;
    gzip_types text/html text/css application/javascript;

    location ~* \.(jsx|html)$ {
        add_header Cache-Control "public, max-age=0, must-revalidate";
    }
}
```

### Speicherplanung

- **NVMe RAID 1** — Betriebssystem, App, Datenbank (falls später ergänzt),
  Ollama-Modelle (ein 3B-Modell belegt ca. 2 GB)
- **HDD** — Backups, Logs, hochgeladene Projekte

---

## 3. Cloudflare

### DNS & Proxy

1. Domain in Cloudflare aufnehmen, Nameserver beim Registrar umstellen
2. `A`-Record auf deine Server-IP, **Proxy aktiviert** (orange Wolke)
3. SSL/TLS-Modus auf **Full (strict)** setzen

### Empfohlene Einstellungen

| Bereich | Einstellung |
|---|---|
| Speed → Optimization | Auto Minify (HTML, CSS, JS) |
| Caching → Configuration | Standard, Browser-TTL 4 Stunden |
| Security → Bots | Bot Fight Mode aktiv |
| Network | HTTP/3 (QUIC) aktiv |

### Turnstile (Botschutz)

Schützt Registrierung und Login vor automatisierten Anmeldungen.

1. Cloudflare-Dashboard → **Turnstile** → *Add Site*
2. Domain eintragen, Widget-Modus **Managed**
3. Den **Site Key** in `index.html` eintragen:

```html
<script>window.__TURNSTILE_SITE_KEY__ = "0x4AAAAAAA...";</script>
```

Bleibt das Feld leer, wird das Widget ausgeblendet und die App funktioniert
unverändert weiter — praktisch für lokale Entwicklung.

> **Wichtig:** Der Site Key ist öffentlich und gehört ins Frontend. Der
> **Secret Key** darf niemals in den Client — er wird nur serverseitig zur
> Verifizierung des Tokens gebraucht. Ohne serverseitige Prüfung ist Turnstile
> eine Hürde für Bots, aber kein harter Schutz.

### Cloudflare Pages (Alternative ohne eigenen Server)

```bash
npx wrangler pages deploy .
```

`_headers` und `wrangler.toml` liegen bereits im Projekt.

---

## 4. KI-Anbindung

Die Plattform funktioniert **vollständig ohne KI-Zugang** — dann läuft die
eingebaute lokale Analyse (siehe Abschnitt 5). Optional stehen drei Anbieter
zur Wahl, einstellbar über das Zahnrad-Symbol in der App.

### Variante A — Google Gemini mit mehreren Keys

Das kostenlose Kontingent liegt bei rund **15 Anfragen pro Minute je Key**.
Mehrere Keys aus verschiedenen Konten lassen sich eintragen und werden
automatisch abwechselnd genutzt:

| Keys | Ungefährer Durchsatz |
|---|---|
| 1 | ~15 Anfragen/Min |
| 3 | ~45 Anfragen/Min |
| 5 | ~75 Anfragen/Min |

Läuft ein Key in ein Limit (HTTP 429), wird er **65 Sekunden pausiert** und der
nächste übernimmt. Bei erschöpftem Kontingent (403/402) beträgt die Pause
10 Minuten. Der aktuelle Zustand aller Keys ist in den KI-Einstellungen
sichtbar.

Keys erstellen: [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)

> **Hinweis zu den Nutzungsbedingungen:** Mehrere Konten anzulegen, um Gratis-
> Kontingente zu bündeln, kann gegen die Terms of Service des Anbieters
> verstoßen. Die technische Umsetzung ist neutral — die Entscheidung liegt bei
> dir. Für den Dauerbetrieb ist Variante B die sauberere Lösung.

### Variante B — Eigener Server mit Ollama (empfohlen)

Keine Kosten, keine Limits, keine Daten an Dritte.

```bash
# Installation
curl -fsSL https://ollama.com/install.sh | sh

# Modell laden (ca. 2 GB)
ollama pull qwen2.5-coder:3b

# Dienst so starten, dass der Browser zugreifen darf
OLLAMA_ORIGINS='*' OLLAMA_HOST=0.0.0.0 ollama serve
```

Als systemd-Dienst:

```ini
# /etc/systemd/system/ollama.service.d/override.conf
[Service]
Environment="OLLAMA_ORIGINS=*"
Environment="OLLAMA_HOST=0.0.0.0:11434"
```

In den KI-Einstellungen dann *Eigener Server (Ollama)* wählen und die Adresse
eintragen (z. B. `http://localhost:11434` oder deine Server-URL).

**Modellwahl für CPU-Betrieb (kein Grafikprozessor):**

| Modell | RAM | Geschwindigkeit | Eignung |
|---|---|---|---|
| `qwen2.5-coder:1.5b` | ~2 GB | schnell | kurze Bewertungen |
| `qwen2.5-coder:3b` | ~3 GB | mittel | **empfohlen** |
| `llama3.2:3b` | ~3 GB | mittel | gute Erklärungen |
| `qwen2.5-coder:7b` | ~6 GB | langsam | nur mit viel Geduld |

Auf 4 Kernen ohne Grafikprozessor sind bei einem 3B-Modell etwa 8–15 Token pro
Sekunde realistisch — für die kurzen Bewertungstexte der Plattform (3–4 Sätze)
bedeutet das rund 3–8 Sekunden pro Antwort. Zusätzliche Kerne verbessern das
näherungsweise linear.

> **Wichtig:** Wenn Ollama öffentlich erreichbar ist, unbedingt über Cloudflare
> Access oder eine Firewall absichern — sonst kann jeder deine Rechenleistung
> nutzen.

### Variante C — Anthropic Claude

Beste Qualität, rechnet aber pro Nutzung ab. Auch hier sind mehrere Keys
möglich. Ein Bewertungsvorgang kostet Bruchteile eines Cents.

---

## 5. Die eingebaute lokale Analyse

Ohne jeden externen Zugang bewertet die Plattform Antworten selbst — direkt im
Browser, ohne Netzwerkverkehr. Der Ablauf:

1. **Vorverarbeitung** — Kommentare und Zeichenketten werden entfernt, damit
   ein Kommentar wie `// nutze const` nicht als Lösung durchgeht
2. **Konzepterkennung** — erwartete Bausteine werden über Wortgrenzen und eine
   Synonymtabelle gesucht (`let oder const`, `funktion` → `function`/`def`/`=>`)
3. **Strukturprüfung** — Klammern werden auf Ausgeglichenheit geprüft
4. **Freitextbewertung** — Ausführlichkeit, Satzbau, Fachbegriffe und vor allem
   begründende Sprache („weil", „damit", „dadurch") fließen in die Punktzahl ein

Für den Code-Debugger prüft dieselbe Engine zusätzlich HTML-Tag-Paare,
CSS-Blöcke sowie typische Stolperfallen wie `==` statt `===` oder `var`.

Diese Analyse ist bewusst regelbasiert und damit sofort, kostenlos und
offline verfügbar. Sie ersetzt kein Sprachmodell bei inhaltlich offenen
Antworten — dafür sind die Varianten A–C gedacht.

---

## 6. Zwei Betriebsarten

Die App erkennt beim Start selbst, ob ein Server erreichbar ist
(`GET /api/health`), und passt sich an:

| | Ohne Server | Mit Server |
|---|---|---|
| Konten | Im Browser (localStorage) | PostgreSQL |
| Passwörter | Klartext im Browser | scrypt-Hash mit Salt |
| Sitzung | Kein echter Login | httpOnly-Cookie, Token nur als Hash gespeichert |
| E-Mail-Bestätigung | Code wird angezeigt | Versand per SMTP |
| 2FA | Fester Code | Echtes TOTP für Authenticator-Apps |
| Speicher (2,5 GB) | Nur Anzeige | Verbindlich durchgesetzt |
| KI-Keys | Im Browser hinterlegt | Bleiben auf dem Server |
| Fortschritt | Manipulierbar | Serverseitig gedeckelt |
| Turnstile | Nur Widget | Token wird geprüft |

Der Betrieb ohne Server bleibt sinnvoll zum Ausprobieren — es muss nichts
installiert werden. Für den echten Einsatz ist der Server die richtige Wahl.

**Einrichtung:** siehe [`server/README.md`](server/README.md).

```bash
cd server
npm install
cp .env.example .env       # DATABASE_URL und SESSION_SECRET setzen
npm run migrate
node src/seed.js --email admin@deine-domain.de
npm start
```

### Empfohlene Aufteilung auf deiner Hardware

| Komponente | Ablage | Begründung |
|---|---|---|
| PostgreSQL-Daten | NVMe (RAID 1) | Schnelle Schreibzugriffe, gespiegelt |
| Ollama-Modelle | NVMe | Ladezeit beim Start |
| App + Server | NVMe | Klein, ändert sich selten |
| Backups (`pg_dump`) | HDD | Groß, selten gelesen |
| Logs | HDD | Wachsen stetig |

Tägliches Backup einrichten:

```bash
# /etc/cron.daily/learndeveloping-backup
#!/bin/sh
pg_dump -U learndeveloping learndeveloping | gzip > /mnt/hdd/backups/ld-$(date +\%F).sql.gz
find /mnt/hdd/backups -name 'ld-*.sql.gz' -mtime +30 -delete
```

## 7. Was noch fehlt

- **Passwort vergessen** — bisher lässt sich das Passwort nur bei bestehender
  Anmeldung ändern. Ein Zurücksetzen per E-Mail-Link fehlt noch.
- **Mehrere Serverprozesse** — der Key-Cooldown liegt im Arbeitsspeicher.
  Auf einer Maschine mit einem Prozess ist das korrekt; für mehrere Instanzen
  bräuchte es einen gemeinsamen Zustand (z. B. Redis).

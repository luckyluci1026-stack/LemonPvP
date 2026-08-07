# LearnDeveloping — Betrieb & Deployment

Diese Anleitung beschreibt den Betrieb auf eigener Hardware hinter Cloudflare
sowie die Einrichtung der KI-Anbindung.

---

## 1. Schnellstart (lokal testen)

### Lokal testen — in drei Befehlen

```bash
cd server
npm install
cp .env.local.example .env      # fertige Einstellungen zum Ausprobieren
npm run dev                     # legt die Datenbank an und startet
```

Danach `http://localhost:3000` öffnen. Das war es — keine Datenbank
installieren, kein API-Schlüssel nötig. Ohne KI bewertet die Plattform
vollständig lokal im Browser.

**Nicht die `.env.example` nehmen.** Die ist für den Livebetrieb, und vier
Werte daraus verursachen lokal Fehler, die nichts anzeigt:

| Wert | Was lokal passiert |
|---|---|
| `NODE_ENV=production` | Das Sitzungs-Cookie wird „secure" markiert und gilt nur über HTTPS. Über `http://localhost` wirft der Browser es weg — die Anmeldung klappt scheinbar, aber auf der nächsten Seite bist du abgemeldet. Ohne jede Meldung. |
| `DATABASE_URL=postgres://…` | Der Start scheitert, wenn kein PostgreSQL läuft. Lokal genügt `sqlite:./data/local.db` — SQLite steckt in Node mit drin. |
| `SESSION_SECRET=` (leer) | In der Produktionsfassung bricht der Start damit ab. |
| `TRUST_PROXY=true` | Der Server glaubt dem Header `X-Forwarded-For`. Lokal steht kein Proxy davor, also lässt sich damit die Anfragenbegrenzung umgehen. |

Weitere Handgriffe:

```bash
npm run seed:local     # ein Administrator-Konto anlegen
rm data/local.db       # alles zurücksetzen, beim nächsten Start neu
```

Bestätigungscodes für E-Mails werden nicht verschickt, sondern in die
Serverausgabe geschrieben — dort steht der Code zum Abtippen.

### Variante A — nur die Oberfläche, ohne alles

Die App braucht keinen Build-Schritt, aber einen HTTP-Server (nicht `file://`):

```bash
npx serve            # oder: python3 -m http.server
```

Dann `http://localhost:3000` (bzw. `:8000`) öffnen. Fortschritt wird im
Browser gespeichert, die Bewertung läuft über die eingebaute lokale Analyse.

### Variante B — mit Server, auch auf schwachem Laptop

Ohne PostgreSQL, ohne Konfigurationsdatei:

```bash
cd server && npm install
npm run seed:local -- --email du@example.com --password "deinpasswort"
npm run start:local
```

Nutzt SQLite über Nodes eingebautes `node:sqlite`. Braucht rund 77 MB
Arbeitsspeicher und startet in unter drei Sekunden.

## 1a. Wie Aufgaben bewertet werden

**Lektionen werden ausschließlich lokal geprüft** — im Browser, ohne Netzwerk,
ohne Kosten, in etwa 15 Millisekunden. Es wird dafür keine KI angefragt, auch
wenn eine eingerichtet ist.

Die KI sitzt stattdessen dort, wo Wartezeit unproblematisch ist: als
**Assistent im Code-Editor**. Dort kann man Fragen zum eigenen Code stellen,
und ein paar Sekunden Antwortzeit stören nicht.

| Wo | Womit | Wartezeit |
|---|---|---|
| Lektionen | Lokale Analyse | ~15 ms |
| Editor-Assistent | Gemini Flash | 1–3 s |
| Editor-Assistent | Ollama (eigener Server) | 10–60 s |

### Was die lokale Analyse leistet

Sie durchsucht den Code nicht nach Textbausteinen, sondern zerlegt ihn:

1. **Tokenisieren** — Kommentare und Zeichenketten werden entfernt. Eine
   auskommentierte Lösung zählt dadurch nicht als Lösung.
2. **Strukturieren** — Deklarationen, Funktionen, Aufrufe und die
   Klammer-Balance werden erfasst (bei HTML stattdessen die Tag-Paare).
3. **Abgleichen** — Erwartete Bausteine werden gegen diese Struktur geprüft.
4. **Bewerten** — Punktzahl plus konkret formuliertes Feedback.

Dadurch entstehen Rückmeldungen, die auf den tatsächlichen Fehler zeigen:

| Eingabe | Rückmeldung |
|---|---|
| `let farbe = "blau"` statt `const` | „Du hast `let` verwendet — die Aufgabe verlangt `const`." |
| `const lieblingsfrabe = …` | „Du hast `lieblingsfrabe` geschrieben — erwartet wird `lieblingsfarbe`." |
| `// const farbe = "blau"` | „Bisher stehen dort nur Kommentare — der eigentliche Code fehlt noch." |
| `def gruss(name)` ohne `:` | „Doppelpunkt fehlt: Nach der Parameterliste muss ein `:` stehen." |
| `<h1>Titel` ohne Schluss-Tag | „`<h1>` wird geöffnet, aber nie geschlossen." |

Ein ausdrücklich verlangtes Schlüsselwort oder ein geforderter Variablenname
gilt als wesentlich — fehlt er, ist die Aufgabe nicht gelöst, auch wenn
rechnerisch genug andere Bausteine vorhanden wären.

Bei Freitextaufgaben zählt nicht die Wortzahl, sondern ob wirklich begründet
wird. Fragt die Aufgabe nach dem *Warum*, reicht eine reine Beschreibung
nicht; wer die Frage nur umformuliert, wird ebenfalls erkannt.

Der Editor nutzt dieselbe Engine für seine Fehlerprüfung und findet dabei auch
dateiübergreifende Fehler — etwa ein `getElementById("btn")`, für das im HTML
kein passendes Element existiert.

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
eingebaute lokale Analyse (siehe Abschnitt 5). Optional stehen vier Anbieter
zur Wahl (`AI_PROVIDER`): `gemini`, `anthropic`, `openrouter` und `ollama`.

### Rollen: jede Aufgabe ihr eigenes Modell

Statt eines einzigen Anbieters bekommt jede Rolle ihren eigenen:

| Rolle | Was sie tut | Einstellungen |
|---|---|---|
| `assist` | Assistent in der IDE | `AI_ASSIST_PROVIDER`, `AI_ASSIST_MODEL` |
| `assistPro` | Stärkerer Assistent auf Knopfdruck | `AI_PRO_PROVIDER`, `AI_PRO_MODEL` |
| `verify` | Prüfung der Lektionsantworten | `AI_VERIFY_PRIMARY`, `AI_VERIFY_FALLBACK` |

Ein Aufbau, bei dem jede Rolle auf dem passenden Modell läuft:

```bash
AI_ASSIST_PROVIDER=gemini    AI_ASSIST_MODEL=gemma-4-31b-it
AI_PRO_PROVIDER=groq         AI_PRO_MODEL=llama-3.3-70b-versatile
AI_VERIFY_PRIMARY=ollama     OLLAMA_MODEL=gemma-4-e4b
AI_VERIFY_FALLBACK=gemini
```

Die IDE läuft dann über Googles Kontingent, der Profi-Assistent über Groq, und
die Lektionsprüfung bleibt auf dem eigenen Server — dort fällt kein Kontingent
an, und die Antworten der Lernenden verlassen das Haus nicht.

### Schnelltest der Einrichtung

Bevor irgendetwas in der App landet:

```bash
cd server
npm run ai:models            # welche Google-Modelle dein Schlüssel wirklich kennt
npm run ai:test              # alle eingestellten Rollen
npm run ai:test -- alle      # jeden Anbieter
npm run ai:test -- gemini    # gezielt einen
```

`ai:models` ist der erste Griff, wenn etwas nicht läuft: Es fragt Google, welche
Modell-IDs dieser Schlüssel benutzen darf. Was dort nicht steht, gibt es für
diesen Zugang nicht — und die Schreibweise zählt (`gemma-4-31b-it` ist etwas
anderes als `gemma-4-31b`).

Der Test geht durch dieselbe Kette wie im Betrieb — derselbe Systemprompt,
dieselbe Auswertung — und schickt zwei Proben: eine richtige Lösung und lose
Wörter, die nur so aussehen. Ein Modell, das beide gleich bewertet, taugt für
die Prüfung nicht. Es wird weder ein laufender Server noch eine Datenbank
gebraucht; Fehler werden in Klartext übersetzt (falscher Schlüssel, unbekannte
Modell-ID, Kontingent erschöpft).

### Variante A — Google mit eigenem API-Key (Vorgabe)

```bash
GEMINI_API_KEYS=AIza...
GEMINI_MODEL=gemma-4-31b-it     # oder gemini-2.0-flash
AI_VERIFY_PRIMARY=gemini
```

Keys erstellen: [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)

`gemma-4-31b-it` läuft über dieselbe Schnittstelle wie die Gemini-Modelle und
unterstützt dort auch Systemanweisungen — der Aufruf im Code ist für beide
derselbe. Das Modell ist kostenlos nutzbar und bringt ein Kontextfenster von
256k Token mit, was für die Antwortprüfung mehr als ausreicht.

### Variante A1 — Gemini mit mehreren Keys

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

### Variante A3 — Groq (Profi-Agent in der IDE)

```bash
GROQ_API_KEYS=gsk_...
GROQ_MODEL=llama-3.3-70b-versatile
AI_PRO_PROVIDER=groq
```

Schlüssel: [console.groq.com](https://console.groq.com)

Beim kostenlosen Kontingent ist **nicht die Zahl der Anfragen der Engpass,
sondern die Token**: 30 Anfragen pro Minute und 1.000 pro Tag klingen
großzügig, aber 12.000 Token pro Minute sind mit einem langen Verlauf plus
viel Code im Editor nach wenigen Fragen aufgebraucht. Deshalb bekommt der
Profi-Agent weniger Verlauf (`AI_PRO_HISTORY`, Vorgabe 4 statt 8) und weniger
Code mit als der normale Assistent. In der Oberfläche ist er ein Schalter, den
man bewusst umlegt — nicht die Voreinstellung.

### Variante A2 — OpenRouter (als Ersatzanbieter)

Ein Zugang, viele Modelle, OpenAI-kompatible Schnittstelle. Interessant vor
allem wegen der kostenlos nutzbaren Modelle.

```bash
OPENROUTER_API_KEYS=sk-or-...
OPENROUTER_MODEL=google/gemma-4-31b-it:free
AI_VERIFY_FALLBACK=openrouter
```

Drei Punkte, die vorher geklärt sein sollten:

1. **Die Modell-ID selbst nachschlagen.** Der Katalog ändert sich laufend.
   IDs aus Blogposts, Foren oder von einem Chatbot stimmen häufig nicht —
   OpenRouter antwortet dann mit 404, und die Antwortprüfung fällt still auf
   die lokale Analyse zurück. Deshalb ist im Code auch keine Vorgabe
   hinterlegt: Ohne gesetzte `OPENROUTER_MODEL` gilt der Anbieter als nicht
   bereit und wird gar nicht erst gefragt.
2. **Datenschutz prüfen.** Für die kostenlosen Modelle verlangt OpenRouter in
   der Regel, dass Anfragen zum Training verwendet werden dürfen. Hier gingen
   Antworten von Lernenden mit — an einer Schule ist das eine bewusste
   Entscheidung, keine Nebensache. Die Einstellung findet sich in den
   Privacy-Einstellungen des Kontos.
3. **Kontingente sind eng.** Bei `google/gemma-4-31b-it:free` sind es etwa
   20 Anfragen pro Minute und 200 pro Tag — und zwar **pro Konto, nicht pro
   Nutzer**. Deshalb ist `AI_VERIFY_GLOBAL_PER_DAY` (Vorgabe 180) die
   wichtigere Bremse, nicht das persönliche Kontingent. Für eine ganze Klasse
   reicht die freie Stufe allein nicht.

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
| `qwen2.5-coder:1.5b` | ~2 GB | schnell | **schwache Laptops** |
| `qwen2.5-coder:3b` | ~3 GB | mittel | **empfohlen für den Server** |
| `llama3.2:3b` | ~3 GB | mittel | gute Erklärungen |
| `qwen2.5-coder:7b` | ~6 GB | langsam | nur mit viel Geduld |

**Auf schwacher Hardware zusätzlich einstellen** (in `.env`):

```
OLLAMA_MODEL=qwen2.5-coder:1.5b
OLLAMA_KEEP_ALIVE=30m     # Modell geladen lassen statt jedes Mal neu einlesen
OLLAMA_MAX_TOKENS=300     # Bewertungen sind ohnehin nur 3-4 Sätze
OLLAMA_CONTEXT=2048       # kleinerer Kontext spart spürbar Rechenzeit
OLLAMA_THREADS=2          # bei sehr wenigen Kernen sinnvoll zu begrenzen
AI_WARMUP=true            # Modell beim Serverstart vorladen
```

`OLLAMA_KEEP_ALIVE` bringt den größten Gewinn: Ohne diese Einstellung entlädt
Ollama das Modell nach fünf Minuten und jede Anfrage zahlt das Einlesen von
mehreren hundert Megabyte erneut.

Weil die Sofort-Bewertung ohnehin unmittelbar erscheint, stört eine langsame
Ollama-Antwort im Hintergrund nicht — sie schärft das Ergebnis nur nach.

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

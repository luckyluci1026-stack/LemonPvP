# BuckSMP – Admin-Panel

Mit dem Admin-Panel änderst du die Website direkt im Browser. Die Änderungen sind **sofort live** – du musst nichts mehr neu hochladen.

| Bereich | Was du dort machst |
|---|---|
| **Übersicht** | Schnellzugriff und die letzten Änderungen |
| **Wiki** | Artikel schreiben, bearbeiten, sortieren, löschen, Kategorien umbenennen, Weiterleitungen anlegen |
| **Aura Moments** | Videos oder Bilder für Platz 1–10 hochladen, ersetzen, entfernen, Plätze tauschen, Titel/Spieler/Beschreibung eintragen |
| **Code** | HTML, CSS und JavaScript der Website bearbeiten – nur sichtbar, wenn du ihn unter „Einstellungen“ einschaltest |
| **Einstellungen** | Code-Editor an/aus, Passwort ändern, andere Geräte abmelden, Wiki-Seiten neu erzeugen |

Das Admin-Panel findest du unter **`/admin/`**, also zum Beispiel `https://bucksmp.de/admin/`. Auf der Website selbst ist es nirgends verlinkt.

## Starten

Voraussetzung ist **Node.js 18 oder neuer** (am besten Node 22). Es gibt **keine npm-Pakete** und nichts zu installieren.

```
node server.js
```

Das startet die Website **und** das Admin-Panel. Standardmäßig läuft beides auf Port 8080: `http://localhost:8080/` und `http://localhost:8080/admin/`.

Auf einem Hosting (zum Beispiel einem Node.js-Server im Panel):

1. Den kompletten Inhalt der `index.zip` hochladen – also auch die Ordner `server/` und `admin/` und die Datei `server.js`.
2. Als Startdatei **`server.js`** eintragen und Node 22 auswählen.
3. Den Server starten und in die Konsole schauen: Dort steht beim allerersten Start das Admin-Passwort.

Der Port wird automatisch aus `PORT` oder `SERVER_PORT` übernommen – so wie es die meisten Hosting-Panels vorgeben.

## Das Passwort

- Beim **allerersten Start** erzeugt der Server ein zufälliges Passwort und zeigt es **einmal** in der Konsole an, zum Beispiel `k7mp-2qfa-x9dw-4hte`. Notier es dir.
- Ändern kannst du es danach im Admin-Panel unter **Einstellungen → Passwort ändern**.
- **Passwort vergessen?** Die Datei `daten/zugang.json` löschen und den Server neu starten – dann steht ein neues Passwort in der Konsole.
- Wer das Passwort lieber selbst festlegen will, startet beim allerersten Mal mit `ADMIN_PASSWORT=dein-passwort node server.js` (mindestens 8 Zeichen).
- Nach einem Neustart des Servers musst du dich neu anmelden. Nach 5 falschen Versuchen ist die Anmeldung für 15 Minuten gesperrt.

## Wo deine Änderungen gespeichert werden

Alles, was du im Admin-Panel änderst, landet im Ordner **`daten/`**:

| Datei | Inhalt |
|---|---|
| `daten/wiki.json` | alle Wiki-Artikel, Kategorien und Weiterleitungen |
| `daten/moments.json` | Titel, Spieler und Beschreibung der Aura Moments |
| `daten/zugang.json` | das Admin-Passwort – nicht im Klartext, sondern nur als unumkehrbarer Hash |
| `daten/einstellungen.json` | ob der Code-Editor an ist |
| `daten/verlauf.json` | die letzten Änderungen für die Übersicht |
| `daten/sicherungen/` | automatische Sicherungen aus dem Code-Editor |

Die hochgeladenen Clips liegen in `top/10/aura-moments/media/` (`1.mp4`, `2.jpg` …).

Der Ordner `daten/` wird **nie** über das Internet ausgeliefert, steht nicht in Git und ist auch **nicht** in der `index.zip`. Beim ersten Start legt der Server ihn selbst an und übernimmt dabei die vorhandenen Wiki-Artikel aus den Wiki-Seiten.

## Eine neue Version der Website hochladen

Du kannst wie bisher eine neue `index.zip` hochladen und entpacken. Danach den Server **einmal neu starten**. Beim Start schreibt er die Wiki-Seiten und die Aura-Moments-Texte wieder aus `daten/` – deine Änderungen aus dem Admin-Panel bleiben also erhalten, auch wenn die Zip ältere Wiki-Seiten enthält. Wichtig ist nur: den Ordner `daten/` und die Clips in `top/10/aura-moments/media/` nicht löschen.

## Gut zu wissen

- **Wiki:** Die Seiten unter `wiki/` werden aus den Artikeln erzeugt. Änderungen direkt in diesen HTML-Dateien werden beim nächsten Speichern überschrieben – deshalb fehlt der Ordner auch im Code-Editor. Wird die Adresse eines Artikels geändert oder ein Artikel gelöscht, legt das Panel automatisch eine Weiterleitung an, damit alte Links weiter funktionieren.
- **Aura Moments:** Videos als MP4 (am besten H.264), höchstens 300 MB. Bilder (JPG, PNG, WebP) wandelt das Panel automatisch in JPG um und verkleinert sie auf höchstens 1920 Pixel.
- **Code-Editor:** Vor jedem Speichern wird der alte Stand gesichert (die letzten 20 pro Datei). Geht etwas kaputt: Datei öffnen, unten bei „Sicherungen“ einen älteren Stand wiederherstellen. Solange der Code-Editor ausgeschaltet ist, kann niemand über das Panel Code verändern.
- **Navigation und Fußzeile** der Wiki-Seiten stehen in `server/vorlage.js`. Wer sie auf den anderen Seiten ändert, sollte sie dort genauso anpassen.
- **HTTPS:** Das Passwort wird beim Anmelden an den Server geschickt. Die Website sollte deshalb über `https://` laufen (zum Beispiel über einen Reverse-Proxy oder Cloudflare).
- **Ohne Node:** Die Website funktioniert auch auf einem ganz normalen Webspace – nur das Admin-Panel nicht, weil dort kein Server läuft, der Änderungen speichern kann.

## Einstellungen beim Start

| Variable | Bedeutung | Standard |
|---|---|---|
| `PORT` oder `SERVER_PORT` | Port der Website | `8080` |
| `ADMIN_PASSWORT` | Passwort beim allerersten Start | zufällig |
| `MAX_UPLOAD_MB` | größte erlaubte Datei bei Aura Moments | `300` |
| `HINTER_PROXY` | auf `1` setzen, wenn ein Reverse-Proxy davor läuft (für die Sperre nach falschen Passwörtern) | aus |

Unter Windows in PowerShell setzt man sie so: `$env:PORT=8081; node server.js`

## Für Entwickler

| Datei | Aufgabe |
|---|---|
| `server.js` | startet den Webserver |
| `server/dateien.js` | liefert die Website aus (nur erlaubte Ordner und Dateitypen, 404-Seite, Video-Streaming) |
| `server/api.js` | alle Aktionen unter `/admin/api/` |
| `server/anmeldung.js` | Passwort, Sitzungen, Sperre nach falschen Versuchen |
| `server/wiki.js` | Wiki-Daten und Erzeugen der Wiki-Seiten |
| `server/vorlage.js` | Kopf, Navigation und Fußzeile der erzeugten Seiten |
| `server/moments.js` | Aura Moments: Texte, Uploads, Tauschen, Entfernen |
| `server/code.js` | Code-Editor mit Sicherungen |
| `admin/` | die Oberfläche des Admin-Panels |

Alle Wiki-Seiten neu erzeugen geht auch ohne Browser: `node server/wiki.js`

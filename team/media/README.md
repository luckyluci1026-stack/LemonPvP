# Team – Profilbilder hochladen

Bild für ein Team-Mitglied hinzufügen: Datei hier in diesen Ordner legen, benannt nach der `id` aus dem `TEAM`-Array in `assets/script.js` (nicht nach dem Namen – die `id` bleibt stabil, auch wenn sich der angezeigte Name mal ändert).

| id (im TEAM-Array) | Datei |
|---|---|
| `lemon` | `lemon.jpg` (oder `lemon.png`) |
| `pneu` | `pneu.jpg` (oder `pneu.png`) – noch kein Bild hochgeladen |
| `owner` | `owner.jpg` (oder `owner.png`) |
| `manager` | `manager.jpg` (oder `manager.png`) |
| `admin` | `admin.jpg` (oder `admin.png`) |
| `mod` | `mod.jpg` (oder `mod.png`) – noch kein Bild hochgeladen |
| `sup` | `sup.jpg` (oder `sup.png`) – gleiches Bild wie `manager.png`, weil beide den Standard-Skin haben |

- `.jpg` wird zuerst versucht, dann `.png`. Gibt es keins von beiden, erscheint ein „?“-Kopf in der Farbe der Rolle – kein Fehler, nichts kaputt.
- Am besten sehen **PNG-Bilder mit durchsichtigem Hintergrund** aus: Dann steht der Kopf direkt auf dem farbigen Hintergrund der Rolle. Die vorhandenen Bilder sind schon so freigestellt (960 × 540 Pixel).
- Neues Mitglied: zuerst einen Eintrag `{ id: '...', name: '...', rolle: '...' }` im `TEAM`-Array in `assets/script.js` ergänzen (die `id` frei wählen) – danach reicht auch hier wieder nur die Bilddatei mit passendem Namen.
- Jedes Mitglied hat außerdem eine eigene Profilseite unter `team/<name>/` (Name klein geschrieben, ohne Punkt davor – zum Beispiel `team/erbse/`). Für ein neues Mitglied einfach einen vorhandenen Profil-Ordner kopieren, umbenennen und Name, Rolle und Texte in der `index.html` anpassen.
- Läuft die Website mit `node server.js`, geht das auch ohne Hochladen: Im Admin-Panel unter `/admin/` den Code-Editor einschalten (Einstellungen) und dort `assets/script.js` oder die Profilseite unter `team/<name>/index.html` bearbeiten.

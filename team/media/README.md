# Team – Profilbilder hochladen

Bild für ein Team-Mitglied hinzufügen: Datei hier in diesen Ordner legen, benannt nach der `id` aus dem `TEAM`-Array in `assets/script.js` (nicht nach dem Namen – die `id` bleibt stabil, auch wenn sich der angezeigte Name mal ändert).

| id (im TEAM-Array) | Datei |
|---|---|
| `lemon` | `lemon.jpg` (oder `lemon.png`) |
| `owner` | `owner.jpg` (oder `owner.png`) |
| `manager` | `manager.jpg` (oder `manager.png`) |
| `admin` | `admin.jpg` (oder `admin.png`) |
| `sup` | `sup.jpg` (oder `sup.png`) |

- `.jpg` wird zuerst versucht, dann `.png`. Gibt es keins von beiden, bleibt das generische Platzhalter-Icon stehen – kein Fehler, nichts kaputt.
- Neues Mitglied: zuerst einen Eintrag `{id:'...', name:'...', rolle:'...'}` im `TEAM`-Array in `assets/script.js` ergänzen (die `id` frei wählen) – danach reicht auch hier wieder nur die Bilddatei mit passendem Namen.

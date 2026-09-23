# Team – Profilbilder hochladen

Bild für ein Team-Mitglied hinzufügen: Datei hier in diesen Ordner legen, benannt nach der `id` aus dem `TEAM`-Array in `assets/script.js` (nicht nach dem Namen – die `id` bleibt stabil, auch wenn sich der angezeigte Name mal ändert).

| id (im TEAM-Array) | Datei |
|---|---|
| `owner` | `owner.jpg` (oder `owner.png`) |
| `admin` | `admin.jpg` |
| `mod1` | `mod1.jpg` |
| `mod2` | `mod2.jpg` |

- `.jpg` wird zuerst versucht, dann `.png`. Gibt es keins von beiden, bleibt das generische Platzhalter-Icon stehen – kein Fehler, nichts kaputt.
- Neues Mitglied: zuerst einen Eintrag `{id:'...', name:'...', rolle:'...'}` im `TEAM`-Array in `assets/script.js` ergänzen (die `id` frei wählen, z.B. `mod3`) – danach reicht auch hier wieder nur die Bilddatei mit passendem Namen.

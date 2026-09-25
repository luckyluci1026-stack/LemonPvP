# Aura Moments – Clips hochladen

Am einfachsten geht alles im **Admin-Panel** unter `/admin/` → „Aura Moments“: Video oder Bild hochladen, ersetzen oder entfernen, Plätze tauschen und Titel, Spieler und Beschreibung eintragen. Mehr dazu steht in `ADMIN.md` im Hauptordner.

Ohne Admin-Panel geht es auch von Hand: Datei in diesen Ordner legen, benannt nach dem Platz (1–10).

| Platz | Video | oder Bild |
|---|---|---|
| 1 | `1.mp4` | `1.jpg` |
| 2 | `2.mp4` | `2.jpg` |
| ... | ... | ... |
| 10 | `10.mp4` | `10.jpg` |

- Das Video wird zuerst versucht. Gibt es zum Beispiel `3.mp4`, wird `3.jpg` ignoriert.
- Gibt es für einen Platz weder `.mp4` noch `.jpg`, zeigt die Seite dort „Clip folgt“ statt eines Fehlers.
- Der Dateiname muss genau `<Platz>.mp4` bzw. `<Platz>.jpg` sein (Kleinbuchstaben, keine Leerzeichen).
- Titel, Spieler und Beschreibung stehen in `top/10/aura-moments/moments.js`. Läuft die Website mit `node server.js`, trägst du sie im Admin-Panel ein – die Datei wird dann beim Start aus `daten/moments.json` neu geschrieben.

# Aura Moments – Clips hochladen

Neuen Moment hinzufügen: Datei hier in diesen Ordner legen, benannt nach dem Platz (1–10). Sonst ist nichts zu tun – kein HTML, kein JS anfassen.

| Platz | Video | oder Foto |
|---|---|---|
| 1 | `1.mp4` | `1.jpg` |
| 2 | `2.mp4` | `2.jpg` |
| ... | ... | ... |
| 10 | `10.mp4` | `10.jpg` |

- Video wird zuerst versucht. Gibt es z.B. `3.mp4`, wird `3.jpg` ignoriert.
- Gibt es für einen Platz weder `.mp4` noch `.jpg`, zeigt die Seite dort einen "Noch kein Clip hochgeladen"-Platzhalter statt eines Fehlers.
- Dateiname muss genau `<Platz>.mp4` bzw. `<Platz>.jpg` sein (Kleinbuchstaben, keine Leerzeichen).
- Optional: Titel/Spieler/Beschreibung pro Platz lassen sich im `MOMENTS`-Array in `assets/script.js` eintragen – für die Anzeige des Clips selbst aber nicht nötig.

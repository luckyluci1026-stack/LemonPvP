# Plugin-Katalog

Alles, was hier als `.jar` liegt, steht im Panel zur Auswahl. Ein Klick, und
die Datei landet im `plugins/`-Ordner des Servers.

In diesem Repo liegen die fertigen Plugins schon in `../../dist/` — das Portal
nimmt diesen Ordner von selbst, wenn hier keine Jars liegen. Willst du einen
anderen Ordner:

```bash
KATALOG_DIR=/pfad/zu/den/jars node start.js
```

Beim Start steht im Terminal, welcher Ordner es geworden ist.

`katalog.json` gibt den Plugins Namen und Beschreibung. Der Schlüssel ist der
Dateiname ohne Version: `BetterSMP-1.0.0.jar` → `BetterSMP`. Ohne Eintrag
erscheint das Plugin trotzdem, dann eben nur mit seinem Dateinamen.

Auf einem **Knoten** liest der Daemon seinen eigenen Katalog — die Jars müssen
also auf der Maschine liegen, auf der die Server laufen, nicht beim Portal.

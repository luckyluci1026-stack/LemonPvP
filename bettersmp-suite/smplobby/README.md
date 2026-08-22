# SMPLobby

Das Plugin für den **Hub** deines Netzwerks — den Server, auf dem alle
landen, bevor sie sich einen SMP aussuchen.

Es gehört auf den **Lobby-Server**. Nicht auf die SMPs, nicht auf den
Proxy. Der Proxy (`SMPProxy`) schickt die Spieler hierher; alles Weitere
passiert hier.

## Was es macht

| | |
|---|---|
| **Schutz** | Kein Abbauen, kein Bauen, kein Schaden, kein Hunger, kein Wetter, feste Uhrzeit. Alles einzeln abschaltbar. Wer `smplobby.bauen` hat, kommt an allem vorbei. |
| **Spawn** | `/setspawn` setzt ihn, `/spawn` bringt dich hin. Wer ankommt, stirbt oder hinunterfällt, landet dort. |
| **Schnellleiste** | Frei konfigurierbare Gegenstände: Server-Wähler, Spieler ausblenden, Regeln. |
| **Server-Wähler** | Ein Fenster mit deinen Servern und ihren Spielerzahlen. Klick → der Proxy schickt dich hin. |
| **Spieler ausblenden** | Ein Klick, und die anderen sind weg — nur für dich. |
| **Anzeigetafel** | Sidebar mit Name, Spielern in der Lobby und im Netzwerk. Flackerfrei. |
| **Doppelsprung** | Zweimal Leertaste, und man fliegt ein Stück. |

## Einrichten

1. Jar nach `plugins/` auf den **Lobby-Server**, starten.
2. Stell dich hin, wo der Spawn sein soll: `/setspawn`
3. In `plugins/SMPLobby/config.yml` die Server unter `waehler.eintraege`
   eintragen. **Wichtig:** `server:` muss exakt so heißen wie in der
   `velocity.toml` unter `[servers]` — sonst passiert beim Klick nichts.
4. `/smplobby reload`

### Damit der Wähler funktioniert

Das Plugin bittet den Proxy, den Spieler weiterzuschicken. Dafür muss der
Lobby-Server wirklich hinter dem Proxy stehen:

- **Velocity:** `player-info-forwarding-mode = "modern"` in der
  `velocity.toml`
- **Lobby-Server:** in `config/paper-global.yml` unter `proxies.velocity`
  `enabled: true` und dasselbe `secret`

Ohne Proxy läuft alles andere weiter — nur die Serverauswahl meldet dann
„Der Proxy antwortet nicht."

## Rechte

| Recht | Standard | Wofür |
|---|---|---|
| `smplobby.spawn` | alle | `/spawn` |
| `smplobby.admin` | op | `/setspawn`, `/smplobby` |
| `smplobby.bauen` | op | Umgeht den kompletten Lobby-Schutz |

## Warum die Spielerzahlen manchmal „–" zeigen

Sie kommen vom Proxy und werden alle fünf Sekunden nachgefragt. Solange
noch keine Antwort da war, steht dort ein Strich. Eine erfundene 0 wäre
schlimmer — dann glaubt man, der Server sei leer.

Die Anfrage läuft über die Verbindung eines Spielers. Ist niemand in der
Lobby, gibt es also auch keine neuen Zahlen — was nichts ausmacht, denn
dann schaut auch niemand hin.

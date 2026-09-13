# AntiSwear

Chat-Filter mit Umgehungsschutz, Punkte-Stufen mit eigener eingebauter
Kurzzeit-Stummschaltung und optionalen Konsolenbefehlen für weitergehende
Strafen. Eigenständig, ohne andere Plugins zu berühren oder zu benötigen.

## Umgehungsschutz

Drei Tricks werden vor dem Abgleich automatisch rausgerechnet - einzeln
abschaltbar in `config.yml` (`erkennung.*`):

| Trick | Beispiel | Einstellung |
|---|---|---|
| Leetspeak | `sch3iss3` | `leetspeak` |
| Trennzeichen | `s.c.h.e.i.s.s.e`, `s c h e i s s e` | `trennzeichen` |
| Buchstaben-Wiederholung | `scheeeiiisse` | `wiederholungen` |

Bei der Trennzeichen-Erkennung werden nur **aufeinanderfolgende
Einzelbuchstaben** zusammengefasst, nicht die ganze Nachricht - sonst
würden mehrere kurze, harmlose Wörter hintereinander ("ich sehe dich")
leicht zu falschen Treffern führen.

Wörter UND Chattext durchlaufen dieselbe Normalisierung: Ein Wort mit
eigenem Doppelbuchstaben (z.B. "scheisse") wird beim Laden genauso
zusammengestaucht wie ein gestreckter Chat-Text - beide landen bei
"scheise" und passen wieder zueinander.

## Modus

`erkennung.modus` in `config.yml`:
- **ERSETZEN** (Standard) - nur das/die Wort(e) werden zu Sternchen
  zensiert, der Rest der Nachricht kommt an. Erkennt die Zensur beim
  eigenen Nachprüfen einen nicht sauber erwischten Umgehungsversuch,
  wird stattdessen die **ganze** Nachricht blockiert - lieber das als
  etwas Anstößiges durchrutschen lassen.
- **BLOCKIEREN** - die ganze Nachricht wird immer verschluckt.

## Wortliste

`woerter.yml` (im Plugin-Ordner): Wort → Punkte, je schwerer der
Verstoß, desto mehr Punkte. Kommt mit einer kurzen Beispiel-Startliste -
eigene Wörter nach Bedarf ergänzen, danach `/antiswear reload`.

## Punkte-Stufen

Jeder Treffer zählt seine Punkte auf das Konto des Spielers. Ohne neuen
Verstoß verfallen alle Punkte nach `strikes.verfall-minuten` (Standard
60) wieder auf 0. Bei jedem Verstoß löst nur die **höchste neu
überschrittene** Stufe aus, nicht alle gleichzeitig:

```yaml
strikes:
  verfall-minuten: 60
  stufen:
    3:
      aktion: WARNEN
    6:
      aktion: STUMMSCHALTEN
      dauer: "10m"
    10:
      aktion: BEFEHL
      befehl: "mute %spieler% 1h Wiederholte Beleidigungen (AntiSwear)"
    15:
      aktion: BEFEHL
      befehl: "ban %spieler% Wiederholte schwere Verstoesse (AntiSwear)"
```

- **WARNEN** - nur eine Nachricht an den Spieler.
- **STUMMSCHALTEN** - AntiSwears eigene, eingebaute Kurzzeit-Sperre für
  den Chat (`dauer` wie `30m 12h 7d 2w`). Braucht kein anderes Plugin.
- **BEFEHL** - führt `befehl` über die Konsole aus, `%spieler%` wird
  durch den Spielernamen ersetzt. Hier lässt sich ein Befehl von
  **AdvancedBan** oder jedem anderen gerade installierten Plugin
  eintragen - AntiSwear selbst kennt dessen API nicht, sondern ruft
  einfach den konfigurierten Befehl auf.

Punkte und die eingebaute Stummschaltung sind rein im Speicher - ein
Serverneustart setzt beides zurück. Das ist bewusst kein Bann-Register,
sondern ein Chat-Filter mit Gedächtnis für den laufenden Betrieb.

## Befehle

```
/antiswear reload            - config.yml und woerter.yml neu einlesen
/antiswear check <Text>      - testen, ob ein Text anschlagen würde
/antiswear punkte <Spieler>  - aktuellen Punktestand ansehen
/antiswear reset <Spieler>   - Punkte und Stummschaltung zurücksetzen
```

## Rechte

- `antiswear.bypass` - Chat wird bei dieser Person gar nicht erst
  geprüft (Standard: nur OP)
- `antiswear.notify` - sieht jeden Treffer unzensiert im Team-Hinweis,
  inklusive Punktestand (Standard: nur OP)
- `antiswear.admin` - `/antiswear` nutzen (Standard: nur OP)

Keine Abhängigkeiten außer der Paper-API.

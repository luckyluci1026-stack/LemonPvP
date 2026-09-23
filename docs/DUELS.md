# BuckSMPAC — Duels Edition

Für Duell-, Arena- und Minigame-Server. In der `config.yml`:

```yaml
duels-mode: true
```

Neu starten. In der Konsole steht dann eine Zeile, dass die Duels Edition läuft.

---

## Was das heißt

| | im Duels-Modus |
|---|---|
| Treffer werden abgebrochen | **nein, nie** |
| Rubberband / Setback | **nein** |
| Checks laufen | ja, alle |
| Alerts an dein Team | ja |
| `punishments.yml`, `bans.yml` | ja, bannen normal |
| `/acban` von Hand | ja |

**Die Erkennung bleibt vollständig an.** Was aufhört, ist das Eingreifen.

---

## Warum es das braucht

Zwei Dinge im Anticheat können einen Treffer verschlucken:

1. **Ein Check bricht das Angriffs-Paket ab.** Reach tut das bei Treffern, die es für unmöglich hält.
2. **Ein Setback.** Wird ein Spieler auf seine letzte gültige Position zurückgezogen, lässt Reach *jeden* Angriff fallen, bis der Client aufgeholt hat — sonst könnte jemand mitten im Teleport von überall treffen.

Punkt 2 ist der unangenehme. Setbacks löst unter anderem `Simulation` aus, und der flaggt bei Lag, auf Eis, in Booten und bei Bedrock-Spielern. Ein einziger Fehlalarm reicht, und für ein paar Ticks kommt kein Schlag mehr durch.

Auf einem SMP ist das richtig so. Im Duell nicht: Der verschluckte Treffer entscheidet das Match, und der Spieler kann nicht unterscheiden, ob das eine Cheat-Abwehr war oder ein schlechter Tick. Er weiß nur, dass er getroffen hat und nichts passiert ist.

---

## Was es kostet

Ehrlich gesagt: **Ein Reach-Cheat landet seine Treffer jetzt.** Vorher wurden sie abgebrochen.

Er wird weiter geflaggt und bei der eingestellten Schwelle gebannt — er darf bis dahin nur treffen. Auf einem Duell-Server ist der Tausch meist richtig, weil die Alternative ehrliche Spieler bei jedem Verbindungsruckler bestraft.

Wenn du das nicht willst, ist der Duels-Modus nichts für diesen Server.

---

## Was bewusst anbleibt

Die **Resynchronisation**. Das ist kein Setback als Strafe, sondern die Reparatur eines echten Desyncs zwischen Client und Server. Die abzuschalten würde verschluckte Treffer gegen Spieler tauschen, die dort stehen, wo der Server sie nicht vermutet — und das ist in einem Duell nicht besser.

---

## Ein Jar für alles

Es gibt keine eigene Duels-JAR. Dieselbe Datei auf allen Servern, nur `duels-mode: true` auf den Duell-Servern und `false` auf dem SMP.

Zwei getrennte Builds hätten denselben Code zweimal — zwei Stellen für jeden Fehler, zwei Stellen für jeden Fix, und irgendwann läuft einer der beiden auf einem alten Stand. Der Schalter macht genau dasselbe, ohne diese Falle.

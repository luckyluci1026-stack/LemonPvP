# BuckSMPAC testen, ohne dass jemand zu Unrecht gebannt wird

Ein Anticheat lässt sich nicht am Schreibtisch fertig einstellen. Ob 100 Flags
bei `Simulation` auf **deinem** Server die richtige Zahl sind, hängt an deinen
TPS, an den Pings deiner Spieler, an Bedrock-Spielern, an Elytra-Strecken. Das
weiß man erst, wenn echte Leute darüber gelaufen sind.

Der Testmodus lässt genau das zu: Das AC zählt und meldet ganz normal, bannt
aber niemanden. Stattdessen schreibt es auf, wen es gebannt **hätte**.

---

## 1. Anschalten

In `plugins/BuckSMPAC/config.yml`:

```yaml
punishment-dry-run: true
```

Server neu starten. In der Konsole steht dann eine Zeile, dass der Testmodus
läuft.

Ab jetzt gilt:

| | passiert |
|---|---|
| Alerts an dein Team | ja, wie immer |
| `/grim verbose` | ja, wie immer |
| Verstoß-Historie | ja, wie immer |
| Bans, Kicks, Befehle aus `punishments.yml` / `bans.yml` | **nein** — nur aufgeschrieben |
| Du tippst selbst `/acban` | **bannt weiterhin echt** |

Der letzte Punkt ist Absicht: Wenn du während des Tests einen echten Cheater
siehst, sollst du ihn rauswerfen können.

---

## 2. Laufen lassen

**Mindestens 3–7 Tage**, an normalen Abenden mit normalem Spielerandrang.
Nicht nur eine Stunde — die interessanten Fehlalarme kommen von seltenen
Situationen, nicht von der Standardbewegung.

Sorg dafür, dass in der Zeit vorkommt:

- normales Laufen, Sprinten, Springen, Treppen
- Kämpfe, auch mit schlechtem Ping
- Elytra, Boote, Pferde, Minecarts
- Eis, Slime-Blöcke, Wasser, Lava, Leitern
- Bedrock-Spieler (über Geyser), die sich normal bewegen
- Bridging und Schnellbau
- ein Spieler mit richtig schlechter Verbindung

---

## 3. Was dabei rauskommt

Die Datei:

```
plugins/BuckSMPAC/testmode.log
```

Eine Zeile pro Ban, der nicht stattgefunden hat:

```
[2026-09-20 21:14:02] Lemonightt — punishments.yml Simulation: acban Lemonightt 7d BuckSMPAC > Cheating detected (Simulation)
```

Die Zeilen stehen auch in der Server-Konsole, mit `[TEST]` davor.

---

## 4. Auswerten

Geh die Datei durch und stell dir bei **jedem Namen** eine Frage:

> Ist das jemand, der wirklich gecheatet hat?

- **Ja, alle** → Schwellen passen. Weiter zu Schritt 5.
- **Einzelne Unschuldige** → schick mir die Datei plus die Namen, die nicht
  hingehören. Dann hebe ich gezielt die Schwelle für den Check an, der sie
  erwischt hat.
- **Viele Unschuldige beim selben Check** → der Check ist für deinen Server zu
  streng eingestellt. Trag ihn erstmal in `bans.yml` unter `ignore-checks`
  ein, dann meldet er nur noch.
- **Datei ist leer, obwohl offensichtlich gecheatet wurde** → die Schwellen
  sind zu hoch. Schick mir die Alerts aus der Konsole.

Nützlich zum Zählen, welcher Check am häufigsten auslöst:

```
grep -o 'punishments.yml [A-Za-z]*' testmode.log | sort | uniq -c | sort -rn
```

---

## 5. Scharf schalten

```yaml
punishment-dry-run: false
```

Neu starten. Ab jetzt bannt es echt — und zwar mit den Zahlen, die du vorher
überprüft hast.

---

## Der AutoClicker-Check

Der bannt bewusst **noch gar nicht**, auch außerhalb des Testmodus. In
`punishments.yml` ist seine Ban-Zeile auskommentiert.

Grund: Jeder andere Check hier stammt von upstream und hat Jahre an Praxisdaten
hinter sich. Der AutoClicker ist neuer Code ohne jede. Er würde als Erstes
Butterfly-Clicker erwischen.

Wenn die Alerts über Wochen nur Leute nennen, die wirklich autoclicken, stell
`Autoclicker.max-attack-cps` in der `config.yml` etwas über deinen schnellsten
ehrlichen Klicker und nimm dann das `#` vor der Ban-Zeile weg.

---

## Wer bannt, wer meldet nur

Zwei Stufen. Checks, die sich mit Lag streiten, bannen **gar nicht** — sie
melden, und du entscheidest. Nur die zuverlässigen dürfen selbst bannen, und
zwar erst bei Zahlen, die ein ehrlicher Spieler nicht erreicht.

| Gruppe | ab Flags | Ban |
|---|---|---|
| Reach | 45 | 14 Tage |
| Hitboxes | 45 | 14 Tage |
| Post | 60 | 14 Tage |
| BadPackets | 60 | 14 Tage |
| Misc | 100 | 7 Tage |
| `bans.yml` Gesamt-Leiter | 200 (bei ≥6 verschiedenen Checks) | 14 Tage |

Nur Alarm, kein Ban:

| Gruppe | warum |
|---|---|
| Simulation | Lag, Eis, Boote, Kolben, Bedrock-Bewegung |
| Knockback | jeder Aussetzer entsynchronisiert einen Treffer |
| Combat (Aim) | liest Drehbewegungen; Ruckler sehen verdächtig aus |
| Movement | Fahrzeuge und Elytra werden viel lockerer berechnet |
| Autoclicker | neuer Code ohne Praxisdaten |

Jede dieser Gruppen hat ihre Ban-Zeile weiter in der `punishments.yml`, nur
auskommentiert und mit einem Startwert. Beobachte die Alerts einer Gruppe eine
Woche; wenn wirklich jeder genannte Spieler gecheatet hat, nimm das `#` weg.

Von Hand bannen geht immer: `/acban Steve 7d Grund`. Ohne Dauer gelten 10 Tage,
länger als 14 Tage geht nicht.

---

## Wenn du dich selbst aussperrst

Auf der **Velocity-Konsole**:

```
/acunban DeinName
```

Das geht auch, während du gebannt bist — die Konsole braucht keinen Login.

Und um sofort alle Bans zu stoppen, ohne irgendwas zu tunen:

```yaml
punishment-dry-run: true
```

Neu starten, fertig. Ab dann wird nur noch aufgeschrieben.

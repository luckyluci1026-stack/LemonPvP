# AntiSwear

Chat- und Voice-Moderation für das ganze Netzwerk: starker Wortfilter mit
Umgehungsschutz, Spam-, Werbungs- und Datenschutz-Filter, Punkte-Stufen mit
Stummschaltung, Team-Hinweise, Protokoll – und Moderation für **Simple Voice
Chat** mit `/vcrules`.

**Auf jeden Server installieren** (SMP, Lobby, Duels). Dann gilt die Moderation
überall, `/msg` wird über SMPProxy mitgeprüft, und Stummschaltungen gelten im
ganzen Netzwerk.

## Was geprüft wird

| Wo | Was passiert |
|---|---|
| Chat | Wörter werden zensiert (oder die Nachricht blockiert, siehe `erkennung.modus`) |
| `/msg`, `/r` | SMPProxy fragt AntiSwear auf dem Server des Absenders – gleiche Regeln wie im Chat |
| Schilder | betroffene Zeilen werden zensiert, über mehrere Zeilen verteilte Wörter werden erkannt |
| Bücher | das Buch wird nicht gespeichert (auch der Titel beim Signieren) |
| Amboss | der Name wird nicht übernommen |
| Voice-Gruppen | Gruppen mit anstößigem Namen werden nicht erstellt |

Zusätzlich:

- **Spam:** mehr als 5 Nachrichten in 8 Sekunden oder dieselbe Nachricht
  innerhalb von 30 Sekunden wird blockiert.
- **Werbung:** fremde Server-Adressen, IPs und Discord-Einladungen – auch mit
  Tricks wie `server (dot) de` oder `1 . 2 . 3 . 4`. Erlaubte Seiten stehen in
  `werbung.erlaubte-domains` (z. B. `bucksmp.de`, YouTube).
- **Persönliche Daten:** Handynummern und E-Mail-Adressen werden nicht
  verschickt – Schutz für die Schüler.
- **Großschrift und Zeichenketten:** `HALLO WIE GEHTS` wird leise zu
  `Hallo wie gehts`, `neeeeeeein!!!!!!!!` zu `neeeein!!!!` (keine Strafe).

## Umgehungsschutz

Vor dem Abgleich wird jeder Text auf ein „Skelett“ zurückgerechnet:

| Trick | Beispiel | wird zu |
|---|---|---|
| Zahlen statt Buchstaben | `hur3ns0hn`, `1d10t`, `9eil`, `5ch31ss3` | `hurensohn`, `idiot`, `geil`, `scheisse` |
| Sonderzeichen | `@rschl0ch`, `$chlampe`, `f!ck`, `|diot` | `arschloch`, `schlampe`, `fick`, `idiot` |
| Trennzeichen | `h.u.r.e`, `h u r e n s o h n`, `huren sohn` | `hure`, `hurensohn` |
| Wiederholungen | `scheeeiiiße` | `scheiße` |
| Doppelgänger | kyrillisches `а`/`о`/`е`, griechische Buchstaben, Akzente, unsichtbare Zeichen, `vv` statt `w` | lateinische Buchstaben |
| Farbcodes | `&churensohn` | `hurensohn` |

Harmlose Wörter, die ein Schimpfwort enthalten, stehen in `ausnahmen`
(`woerter.yml`), z. B. *Arschbombe*, *Marsch*, *Schlamper*.

## Wortliste

`woerter.yml`: Wort → Punkte, optional mit Modus:

```yaml
woerter:
  scheis: 1
  hurensohn: 3
  behindert: { punkte: 2, modus: wort }   # nur als ganzes Wort
  nigga: { punkte: 6, modus: teil }       # auch als Teil eines Wortes
ausnahmen:
  - arschbombe
```

Ohne Modus gilt: ab 6 Buchstaben auch als Teil eines Wortes, kürzere nur als
ganzes Wort (mit üblichen Endungen wie *-e*, *-en*, *-s*). Nach Änderungen
`/antiswear reload`.

## Punkte-Stufen

Jeder Verstoß zählt seine Punkte (Wortwahl, Spam `spam.punkte`, Werbung
`werbung.punkte`, Daten `daten.punkte`). Ohne neuen Verstoß verfallen alle
Punkte nach `strikes.verfall-minuten`. Nur die höchste neu erreichte Stufe
löst aus:

```yaml
strikes:
  verfall-minuten: 60
  stufen:
    3:  { aktion: WARNEN }
    6:  { aktion: STUMMSCHALTEN, dauer: "10m" }
    10: { aktion: STUMMSCHALTEN, dauer: "1h" }
    15: { aktion: STUMMSCHALTEN, dauer: "1d" }
```

- **WARNEN** – Nachricht an den Spieler
- **STUMMSCHALTEN** – stumm im ganzen Netzwerk (Chat und `/msg`), `dauer`
  wie `30m 12h 7d 2w` oder `perm`
- **BEFEHL** – führt `befehl` über die Konsole aus (`%spieler%` wird ersetzt)

Das Team (`antiswear.notify`) sieht jeden Treffer mit Ort, Grund, Punktestand
und **Originaltext**. Jeder Verstoß landet außerdem in
`plugins/AntiSwear/protokoll.log` (Datum, Server, Spieler, Ort, Grund,
Punkte, Text).

## Voice-Chat (Simple Voice Chat)

Greift automatisch, sobald Simple Voice Chat auf dem Server läuft. Es wird
**nichts aufgenommen** – moderiert wird über Regeln, Stummschaltungen und
Gruppennamen.

- **`/vcrules`** zeigt die Voice-Regeln mit einem Knopf zum Akzeptieren. Erst
  danach kann man sprechen (zuhören geht immer). Beim ersten Verbinden mit dem
  Voice-Chat kommen die Regeln automatisch, beim Sprechen ohne Zustimmung ein
  Hinweis in der Actionbar. Regeln ändern: `voice.regeln` in `messages.yml`,
  danach `voice.regeln-version` hochzählen – dann müssen alle neu zustimmen.
- **`/vcmute <Spieler> <Dauer|perm> [Grund]`** schaltet im Voice-Chat stumm
  (das Mikrofon wird serverseitig verworfen, der Spieler sieht die Restzeit),
  **`/vcunmute <Spieler>`** hebt es auf, **`/vcmutes`** zeigt alle.
- Namen neuer **Voice-Gruppen** laufen durch den Wortfilter.
- Stummschaltungen und „Regeln akzeptiert“ gelten über SMPProxy im **ganzen
  Netzwerk** (auch für Spieler, die gerade offline sind) und überstehen
  Neustarts (`voice.yml`).

## Befehle

```
/antiswear reload               config.yml, messages.yml und woerter.yml neu einlesen
/antiswear check <Text>         testen, ob ein Text anschlagen würde
/antiswear punkte <Spieler>     Punktestand und Stummschaltung ansehen
/antiswear reset <Spieler>      Punkte und Stummschaltung zurücksetzen (netzwerkweit)
/antiswear stumm <Spieler> <Dauer|perm>   von Hand stummschalten (netzwerkweit)
/vcrules [akzeptieren]          Voice-Regeln (alle)
/vcmute <Spieler> <Dauer|perm> [Grund]
/vcunmute <Spieler>
/vcmutes
```

## Rechte

- `antiswear.bypass` – wird nicht geprüft (Standard: OP)
- `antiswear.notify` – Team-Hinweise mit Originaltext (Standard: OP)
- `antiswear.admin` – `/antiswear` (Standard: OP)
- `antiswear.voice.mute` – `/vcmute`, `/vcunmute`, `/vcmutes` (Standard: OP)

Abhängigkeiten: nur die Paper-API. Simple Voice Chat ist optional.

# FastShop

Einfaches, customizables Shop-/Verkaufssystem (Paper 1.21.11).

## Features
- **`/shop`-GUI** mit Kategorien-Hauptmenü und paginierten Kategorie-Seiten.
  - Linksklick: 1 kaufen · Shift-Links: 64 kaufen
  - Rechtsklick: 1 verkaufen · Shift-Rechts: alle verkaufen
- **`/sell hand|all|gui`**: Item in der Hand, alles Verkaufbare im Inventar oder
  ein Ablege-GUI (Items hineinlegen, Fenster schließen = verkaufen).
- **`/worth`**: zeigt Kauf-/Verkaufswert des Items in der Hand.
- **EssentialsX-/Vault-Economy**: nutzt das vorhandene Economy-System.
- **Voll konfigurierbar** über `shop.yml` (Kategorien, Items, Preise) – ein
  reichhaltiger Standard-Katalog ist bereits enthalten, inklusive `pvp`- und
  `tools`-Kategorien. Die Preise für Erze, Barren und Netherit legt das
  Server-Team fest (Stand September 2026: Diamant 1.250, Netherit-Block
  90.000, Netherit-Rüstung höchstens 15.000). `sell-multiplier` global.
- **Alles unverzaubert**: Im Shop gibt es keine verzauberten Items, und in
  den Menüs leuchtet nichts. Steht in einer älteren `shop.yml` noch
  `enchants:`, entfernt FastShop das beim Start selbst, samt den Namen und
  Beschreibungen dieser Items, und übernimmt deren Preise aus dem
  Standard-Katalog (Bogen 80, Angel 60). Die alte Datei bleibt als
  `shop-vorher.yml` liegen.
- **Schutz**: Bulk-Verkauf ignoriert benannte/verzauberte/beschädigte Items
  (deine Ausrüstung wird nicht versehentlich verkauft).
- `/fastshop additem <Kategorie> <Kaufpreis> <Verkaufspreis>`: Item aus der Hand
  direkt in den Katalog aufnehmen.

## Auktionshaus (`/ah`)
Spieler verkaufen Items an andere Spieler, wie auf DonutSMP. Jede Form von
`/ah` öffnet ein Fenster:

| Eingabe | Fenster |
|---|---|
| `/ah` | Übersicht mit allen Angeboten |
| `/ah diamant` (irgendein Begriff) | Übersicht, schon danach gesucht |
| `/ah search` | Eingabefenster für den Suchbegriff |
| `/ah sell` | Verkaufen-Fenster mit dem Item aus der Hand |
| `/ah sell 2.5k` | Verkaufen-Fenster, Preis schon eingetragen |
| `/ah meine` | Deine Angebote |

- **Kaufen**: In der Übersicht (45 Angebote pro Seite) auf ein Item klicken,
  dann auf das grüne Feld. Das Geld geht sofort an den Verkäufer, auch wenn
  er offline ist. Beim nächsten Join sieht er, was verkauft wurde.
- **Verkaufen**: Im Verkaufen-Fenster unten im eigenen Inventar auf ein Item
  klicken, den Preis mit den grünen und roten Feldern (±10 bis ±100.000)
  oder mit „Preis eintippen“ einstellen, dann „Angebot erstellen“. Als
  Vorschlag steht der Betrag drin, den der Shop für das Item zahlt. Preise
  gehen auch kurz: `1500`, `2.5k`, `1m`, `1.000.000`. Erst der grüne Knopf
  bietet das Item an – vorher bleibt es ganz normal im Inventar.
- **Sortieren und Filtern**: Neueste, Günstigste, Teuerste, Endet bald und
  Kategorien (Blöcke, Werkzeuge, Kampf & Rüstung, Essen, Tränke & Bücher,
  Sonstiges).
- **Suchen**: Klick auf das Schild oder `/ah <Begriff>`. Gesucht wird nach
  Item- und Spielernamen, auf Englisch und Deutsch: „Diamantschwert“,
  „Goldäpfel“ oder „Eisenerz“ finden die richtigen Items.
- **Deine Angebote**: `/ah meine` oder die Endertruhe im Menü. Dort nimmst
  du Angebote zurück und holst abgelaufene Items ab.
- Die Eingabefenster (Suchbegriff, Preis) sind Minecraft-Dialoge
  (Java ab 1.21.6); Bedrock-Spieler sehen sie über Geyser als Formular.
- **Aus der Lobby**: Mit SMPProxy geht `/ah` auch dort. Man wird auf den SMP
  gebracht und das Auktionshaus öffnet sich sofort.
- **Kein Dupe beim Duell**: Sperrt DuelPlus das Inventar für den Wechsel zum
  Duell, reagieren Shop und Auktionshaus nicht mehr, und offene Fenster
  werden vorher geschlossen.
- Ein Angebot läuft 48 Stunden. Danach bleibt das Item sicher liegen, bis
  du es abholst. Es geht nichts verloren.
- Alles liegt in `plugins/FastShop/auktionen.yml` und übersteht Neustarts.

Einstellungen in `config.yml` unter `auktionshaus:` (fehlt der Abschnitt,
gelten diese Werte):
- `dauer-stunden: 48`
- `max-angebote: 50` pro Spieler, abgelaufene zählen mit (`/ah meine` blättert ab 46 Angeboten auf eine zweite Seite).
  Steht in einer älteren `config.yml` noch der alte Standard `10`, stellt FastShop ihn beim Start selbst auf `50`.
- `min-preis: 1`, `max-preis: 1000000000`
- `steuer-prozent: 0` Gebühr, die beim Verkauf einbehalten wird

## Befehle
- `/shop [Kategorie]` (`fastshop.use`)
- `/sell <hand|all|gui>` (`fastshop.sell`)
- `/worth` (`fastshop.worth`)
- `/ah [Begriff | sell [Preis] | search [Begriff] | meine]` (`fastshop.ah`, für alle)
- `/fastshop reload|additem` (`fastshop.admin`)

Konfiguration: `config.yml` (Menü, Multiplikator, Auktionshaus), `shop.yml`
(Katalog), `messages.yml`. Braucht **Vault + EssentialsX** (installiert
BetterSMP automatisch).

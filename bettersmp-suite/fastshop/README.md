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
  `tools`-Kategorien mit fertig verzauberten Waffen/Rüstungen/Werkzeugen
  (`enchants:` pro Item, rein config-gesteuert). Die Preise für Erze, Barren
  und Netherit legt das Server-Team fest (Stand September 2026: Diamant
  1.250, Netherit-Block 90.000, Netherit-Rüstung höchstens 15.000).
  `sell-multiplier` global.
- **Schutz**: Bulk-Verkauf ignoriert benannte/verzauberte/beschädigte Items
  (deine Ausrüstung wird nicht versehentlich verkauft).
- `/fastshop additem <Kategorie> <Kaufpreis> <Verkaufspreis>`: Item aus der Hand
  direkt in den Katalog aufnehmen.

## Auktionshaus (`/ah`)
Spieler verkaufen Items an andere Spieler, wie auf DonutSMP.
- **Anbieten**: Item in die Hand nehmen, `/ah sell <Preis>` tippen. Preise
  gehen auch kurz: `1500`, `2.5k`, `1m`, `1.000.000`.
- **Kaufen**: `/ah` öffnet die Übersicht (45 Angebote pro Seite). Klick auf
  ein Item, dann auf das grüne Feld. Das Geld geht sofort an den Verkäufer,
  auch wenn er offline ist. Beim nächsten Join sieht er, was verkauft wurde.
- **Sortieren und Filtern**: Neueste, Günstigste, Teuerste, Endet bald und
  Kategorien (Blöcke, Werkzeuge, Kampf & Rüstung, Essen, Tränke & Bücher,
  Sonstiges). `/ah search <Begriff>` sucht nach Item- oder Spielernamen.
- **Deine Angebote**: `/ah meine` oder die Endertruhe im Menü. Dort nimmst
  du Angebote zurück und holst abgelaufene Items ab.
- Ein Angebot läuft 48 Stunden. Danach bleibt das Item sicher liegen, bis
  du es abholst. Es geht nichts verloren.
- Alles liegt in `plugins/FastShop/auktionen.yml` und übersteht Neustarts.

Einstellungen in `config.yml` unter `auktionshaus:` (fehlt der Abschnitt,
gelten diese Werte):
- `dauer-stunden: 48`
- `max-angebote: 10` pro Spieler, abgelaufene zählen mit
- `min-preis: 1`, `max-preis: 1000000000`
- `steuer-prozent: 0` Gebühr, die beim Verkauf einbehalten wird

## Befehle
- `/shop [Kategorie]` (`fastshop.use`)
- `/sell <hand|all|gui>` (`fastshop.sell`)
- `/worth` (`fastshop.worth`)
- `/ah [sell <Preis> | search <Begriff> | meine]` (`fastshop.ah`, für alle)
- `/fastshop reload|additem` (`fastshop.admin`)

Konfiguration: `config.yml` (Menü, Multiplikator, Auktionshaus), `shop.yml`
(Katalog), `messages.yml`. Braucht **Vault + EssentialsX** (installiert
BetterSMP automatisch).

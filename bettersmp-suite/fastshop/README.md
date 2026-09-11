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
  (`enchants:` pro Item, rein config-gesteuert). Preise in `ores` (ab Diamant
  aufwärts) und die Diamant-/Netherit-Ausrüstung sind an echten
  DonutSMP-Marktpreisen ausgerichtet (Stand September 2026) – ein reines
  Preisgefühl, kein Live-Feed, da DonutSMPs Markt spielergetrieben schwankt.
  `sell-multiplier` global.
- **Schutz**: Bulk-Verkauf ignoriert benannte/verzauberte/beschädigte Items
  (deine Ausrüstung wird nicht versehentlich verkauft).
- `/fastshop additem <Kategorie> <Kaufpreis> <Verkaufspreis>`: Item aus der Hand
  direkt in den Katalog aufnehmen.

## Befehle
- `/shop [Kategorie]` (`fastshop.use`)
- `/sell <hand|all|gui>` (`fastshop.sell`)
- `/worth` (`fastshop.worth`)
- `/fastshop reload|additem` (`fastshop.admin`)

Konfiguration: `config.yml` (Menü, Multiplikator), `shop.yml` (Katalog),
`messages.yml`. Braucht **Vault + EssentialsX** (installiert BetterSMP automatisch).

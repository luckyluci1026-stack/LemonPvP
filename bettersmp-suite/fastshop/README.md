# FastShop

Einfaches, customizables Shop-/Verkaufssystem (Paper 1.21.11).

## Features
- **`/shop` wie auf DonutSMP**: kleines Hauptmenü (3 Reihen) mit vier
  Kategorien: **End**, **Nether**, **Gear** und **Essen**. Zu kaufen gibt es
  nur 24 Items, zum Beispiel Enderperlen, Obsidian, Totems, Goldäpfel und
  Essen. End-Kristalle gibt es im Shop nicht. Eine Kategorie mit bis zu
  7 Items öffnet ein kleines Fenster, die Items liegen mittig in einer Reihe.
  - Linksklick: Kaufen/Verkaufen-Fenster · Shift-Links: 64 kaufen
  - Rechtsklick: 1 verkaufen · Shift-Rechts: alle verkaufen
- **Die alten Preise**: Was es vor dem Nerf schon gab, kostet und bringt
  wieder genau so viel wie früher (Enderperle 60/15, Totem 1.500/400,
  Goldapfel 375/80, Diamant verkauft 400, Eisen 45, Bruchstein 1). Neu
  dazugekommene Items (Endertruhe, Shulker-Schale, Ghast-Träne ...) haben
  passende Preise. Nirgends bringt Verkaufen so viel, wie Kaufen kostet.
  Beträge werden auf den Cent gerundet.
- **Verkaufen geht mit jedem Item**:
  - In `shop.yml` steht eine versteckte **Verkaufsliste** (Kategorie
    `verkauf` mit `im-menue: false`) mit festen Preisen für Rohstoffe, Erze,
    Holz, Essen, Mob-Drops und seltene Items, wie früher (Diamant 400,
    Eisen 45, Smaragd 150 ...).
  - Alles andere bewertet FastShop selbst aus den echten Rezepten des
    Servers (Werkbank, Ofen, Steinsäge, Schmiedetisch): Ein gecraftetes Item
    bringt 90 % von dem, was seine Zutaten wert sind. Ein Diamantschwert
    bringt so rund 720, ein Eisenblock 364,50. Durch Craften kann niemand
    Geld vermehren.
  - Was man nicht craften kann und nicht in der Liste steht (Blumen,
    Netherrack, Verrottetes Fleisch ...), bringt 0,10.
  - Nie verkaufbar: Spawner, Spawn-Eier, Drachenei, Zauberbücher,
    beschriebene Bücher, Karten, Grundgestein und andere Admin-Blöcke.
  - Volle Eimer und Tränke bringen nie weniger als der leere Eimer bzw. die
    leere Flasche.
- **`/sell hand|all|gui`**: `/sell hand` und das Ablege-Fenster (`/sell`)
  nehmen jedes Item. `/sell all` verkauft nur, was in der Verkaufsliste
  steht, so landen Werkzeuge, Rüstung und Baublöcke nie aus Versehen im
  Verkauf.
- **`/worth`**: zeigt Kauf- und Verkaufswert des Items in der Hand. Bei
  selbst berechneten Werten steht „(aus den Zutaten berechnet)“ dahinter.
- **EssentialsX-/Vault-Economy**: nutzt das vorhandene Economy-System.
- **Einstellungen** in `config.yml` unter `settings:`: `alles-verkaufen: true`
  (bei `false` gilt nur noch die Verkaufsliste), `standard-wert: 0.1`,
  `rezept-faktor: 0.9`, `sell-multiplier` für alle Verkaufspreise,
  `menu-rows: 3`.
- **Bestehende `shop.yml` wird einmal umgestellt**: Fehlt oben
  `preis-stand: 3` (alte Dateien, auch die generfte mit Stand 2), ersetzt
  FastShop sie beim Start durch den neuen Katalog. Die alte Datei bleibt als
  `shop-vor-donut.yml` liegen, die Konsole sagt Bescheid. Steht das
  Hauptmenü noch auf 5 Reihen (alter Standard), wird es auf 3 verkleinert.
  Danach bleibt `shop.yml`, wie sie ist: Eigene Änderungen (z. B. mit
  `/fastshop additem`) bleiben erhalten.
- **Alles unverzaubert**: Im Shop gibt es keine verzauberten Items, und in
  den Menüs leuchtet nichts. Steht in `shop.yml` noch `enchants:`, entfernt
  FastShop das beim Start selbst, samt den Namen und Beschreibungen dieser
  Items, und übernimmt den Preis aus dem Katalog, wenn das Item dort steht.
  Die alte Datei bleibt als `shop-vorher.yml` liegen.
- **Schutz**: Verkaufen ignoriert benannte, verzauberte und beschädigte
  Items sowie volle Shulkerkisten und Beutel (deine Ausrüstung und der
  Inhalt gehen nicht versehentlich weg). Aus dem Ablege-Fenster kommen sie
  zurück ins Inventar.
- `/fastshop additem <Kategorie> <Kaufpreis> <Verkaufspreis>`: Item aus der
  Hand direkt in den Katalog aufnehmen, zum Beispiel mit
  `/fastshop additem verkauf -1 25` als fester Verkaufspreis ohne Kaufen.

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

Konfiguration: `config.yml` (Menü, Verkaufswerte, Auktionshaus), `shop.yml`
(Katalog und Verkaufsliste), `messages.yml`. Braucht **Vault + EssentialsX** (installiert
BetterSMP automatisch).

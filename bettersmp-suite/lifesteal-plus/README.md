# Lifesteal+

Lifesteal-Plugin mit Herzverlust und BetterSMP-CombatLog-Integration (Paper 1.21.11).

## Features
- **Herz-Transfer**: Killer gewinnt Herzen, Opfer verliert Herzen. Herzen liegen
  auf dem echten `MAX_HEALTH`-Attribut und werden in `data.yml` persistiert
  (auch für Offline-Spieler).
- **Elimination**: Bei 0 Herzen wird der Spieler eliminiert – wahlweise
  **Spectator-Modus** (bis zur Wiederbelebung) oder **temporärer Bann**.
- **Herz-Item** (Rechtsklick = +1 Herz) und **Revive-Totem**, beide mit
  konfigurierbarem Crafting-Rezept. `/withdraw` wandelt Herzen in Items um.
- **Wiederbelebung** per `/revive` oder Revive-Item (kostet ein Herz).
- **Anti-Farm**: Cooldown gegen wiederholte Kills am selben Opfer + optionaler
  Same-IP-Schutz (Alt-Accounts).
- **BetterSMP-CombatLog**: Loggt ein Spieler im Kampf aus, zählt das als Tod –
  Herzverlust für den Logger, Herzgewinn für den Gegner (via
  `PlayerCombatLogEvent`). Doppelte Wertung wird sauber verhindert.
- **PlaceholderAPI**: `%lifesteal_hearts%`, `%lifesteal_maxhearts%`,
  `%lifesteal_health%`, `%lifesteal_eliminated%`, `%lifesteal_status%`.

## Befehle
- `/hearts [Spieler]` – Herzen anzeigen
- `/withdraw [Anzahl]` – Herzen → Item (min. 1 Herz bleibt)
- `/revive <Spieler>` – eliminierten Spieler wiederbeleben
- `/lifesteal reload|set|give|eliminate|revive` – Admin (`lifesteal.admin`)

Konfiguration: `config.yml` (Herzen, Elimination, Items, Anti-Farm, CombatLog),
`messages.yml`.

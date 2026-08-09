# BetterSMP Suite

Sechs eigenständige Paper-Plugins für **Minecraft 1.21.11**, gebaut gegen die
**offizielle PaperMC-API** (aus den Quellen des `ver/1.21.11`-Branches kompiliert).
Ein wiederverwendbares Server-Paket für eigene SMP-Server – neutral gehalten,
den Server-Namen setzt du einmal über `brand` in der `config.yml`.

| Plugin | Zweck | Befehle |
|--------|-------|---------|
| **BetterSMP** | SMP-Kern: MiniMessage-Chat, NoChatReports, AntiCombatLog, **Ban/Mute-System mit Screen**, **/stats** (MariaDB/SQLite), **Ränge mit Gradient-Prefixen**, **Nametags**, **Scoreboard**, **/settings-GUI** + Auto-Installer | `/bettersmp`, `/settings`, `/stats`, `/gban`, `/gunban`, `/gmute`, `/gunmute` |
| **BetterRTP** | Vollständig asynchrones Random-Teleport-Plugin | `/rtp` (`/wild`), `/betterrtp` |
| **Lifesteal+** | Lifesteal mit Herzverlust, Elimination/Revive, BetterSMP-CombatLog | `/hearts`, `/withdraw`, `/revive`, `/lifesteal` |
| **EasyBedrock** | Bedrock-Crossplay (Geyser + Floodgate), Bedrock-Spieler ≈ Java-Spieler an Ressourcen | `/easybedrock` |
| **FastShop** | DonutSMP-artiges `/shop` + `/sell`, komplett in-game editierbar, EssentialsX-Economy | `/shop`, `/sell`, `/worth`, `/fastshop` |
| **SMPContent** | **Eigene Blöcke und Items** (8 Blöcke, 7 Items) mit Rezepten, passend zum Texturepack | `/smpcontent` |

Die externen Begleit-Plugins (EssentialsX, LuckPerms, Vault, PlaceholderAPI, TAB
bzw. Geyser, Floodgate) sind **nicht mitgebündelt**, sondern werden von BetterSMP
bzw. EasyBedrock beim ersten Start automatisch von den **offiziellen Quellen**
heruntergeladen und mit fertigen Configs eingerichtet.

## Neu in dieser Version

- **Ban-/Mute-System** mit eigenem, bedrock-freundlichem Ban-Screen. Gründe mit
  fester Dauer in `bans.yml` / `mutes.yml` – neue Gründe (Dauer + Screen) einfach
  ergänzen. `/gban <Spieler> <Grund>`, `/gunban`, `/gmute`, `/gunmute`.
- **Datenbank**: MariaDB (wenn in `config.yml` aktiviert) oder automatisch SQLite.
  Dort landen Stats, Bans und Mutes. Die Treiber lädt Paper zur Laufzeit selbst.
- **/stats** – Kills, Tode, K/D, Mob-Kills, Spielzeit, Geld u. v. m.
- **Ränge** mit MiniMessage-**Gradient-Prefixen**: Owner (5 Looks), Admin, Mod,
  Sup, default. `/bettersmp ranks` legt sie in LuckPerms an.
- **Nametags** (Gradient-Prefix über dem Kopf + Tab, Name weiß). Liest den
  LuckPerms-Prefix live – **jeder neue LuckPerms-Rang funktioniert sofort**, ohne
  Code-Änderung. TAB-Nametags werden dafür automatisch deaktiviert.
- **Scoreboard** (Sidebar), frei konfigurierbar in `scoreboard.yml` mit
  Platzhaltern für Geld, Ping, Spieler, TPS, Kills, K/D usw.
- EssentialsX-**Kits entfernt**, alles **de-brandet** (`brand`-Wert in der Config).

## Schnellstart

1. Alle fünf Jars aus `dist/` in den `plugins/`-Ordner deines Paper-1.21.11-Servers legen.
2. Server starten. BetterSMP und EasyBedrock laden die fehlenden Begleit-Plugins herunter.
3. Server **einmal neu starten**, damit die neuen Plugins geladen werden.
4. `brand` in `plugins/BetterSMP/config.yml` auf deinen Server-Namen setzen,
   `/bettersmp ranks` für die Standard-Ränge ausführen. Fertig.

> Für MariaDB: in `config.yml` unter `database.mariadb` `enabled: true` und die
> Zugangsdaten eintragen. Sonst wird automatisch SQLite genutzt.

## Selbst bauen

Voraussetzungen: **JDK 21** und **Maven**. Siehe `../BUILDING.md` (die Paper-API
1.21.11 wird lokal aus den offiziellen Quellen gebaut), dann:

```bash
cd bettersmp-suite
mvn clean install -DskipTests
```

## Aufbau

```
bettersmp-suite/
├── compile-stubs/     Compile-Only-Stubs (Vault-Economy, PlaceholderAPI) – nie ausgeliefert
├── bettersmp/         BetterSMP
├── betterrtp/         BetterRTP
├── lifesteal-plus/    Lifesteal+
├── easybedrock/       EasyBedrock
├── fastshop/          FastShop
└── smpcontent/        SMPContent (eigene Blöcke & Items)
```

Jedes Plugin ist eigenständig nutzbar. Lifesteal+ nutzt die CombatLog-API von
BetterSMP, funktioniert aber auch ohne (die Integration schaltet sich dann ab).

# BetterSMP Suite – LemonPvP

Fünf eigenständige Paper-Plugins für **Minecraft 1.21.11**, gebaut gegen die
**offizielle PaperMC-API** (aus den Quellen des `ver/1.21.11`-Branches kompiliert).

| Plugin | Zweck | Befehle |
|--------|-------|---------|
| **BetterSMP** | SMP-Kern: MiniMessage-Chat mit LuckPerms-Prefixen (LPC-Stil), NoChatReports, AntiCombatLog, Join/Quit, **`/settings`-GUI** + Auto-Installer & fertige Configs für EssentialsX, LuckPerms, Vault, PlaceholderAPI, TAB | `/bettersmp`, `/settings` |
| **BetterRTP** | Sehr gutes, **vollständig asynchrones** Random-Teleport-Plugin mit sicherer Positionssuche, Cooldown, Warmup, Welt-Profilen | `/rtp` (`/wild`), `/betterrtp` |
| **Lifesteal+** | Lifesteal mit Herzverlust, Herz-Items, Elimination/Revive, Anti-Farm und **BetterSMP-CombatLog-Integration** | `/hearts`, `/withdraw`, `/revive`, `/lifesteal` |
| **EasyBedrock** | Bedrock-Crossplay in einem Schritt: Geyser + Floodgate (offizielle Downloads) mit **Direktverbindung** → ein Bedrock-Spieler ≈ ein Java-Spieler an Ressourcen | `/easybedrock` |
| **FastShop** | Einfaches, customizables `/shop`-GUI und `/sell` über die **EssentialsX-/Vault-Economy** | `/shop`, `/sell`, `/worth`, `/fastshop` |

Die vier großen externen Plugins (EssentialsX, LuckPerms, Vault, PlaceholderAPI,
TAB bzw. Geyser, Floodgate) sind **nicht mitgebündelt**, sondern werden von
BetterSMP bzw. EasyBedrock beim ersten Serverstart automatisch von den
**offiziellen Quellen** (GitHub-Releases, luckperms.net, download.geysermc.org)
heruntergeladen und mit fertigen Configs eingerichtet. So bleibt jedes Plugin
schlank und lizenzsauber – du musst kaum etwas selbst einstellen.

## Schnellstart

1. Alle fünf Jars aus `dist/` in den `plugins/`-Ordner deines Paper-1.21.11-Servers legen.
2. Server starten. BetterSMP und EasyBedrock laden die fehlenden Begleit-Plugins
   automatisch herunter (Internetzugang vorausgesetzt).
3. Server **einmal neu starten**, damit die nachgeladenen Plugins aktiv werden.
4. Fertig. Feinschliff bei Bedarf über `/settings`, die `config.yml`-Dateien und `/bettersmp ranks`.

> Ist der automatische Download in deiner Umgebung gesperrt, kannst du die
> Begleit-Plugins auch manuell in `plugins/` legen – die Feature-Plugins
> erkennen sie dann und überspringen den Download.

## Selbst bauen

Voraussetzungen: **JDK 21** und **Maven**. Die Paper-API 1.21.11 wird lokal aus
den offiziellen Quellen gebaut (siehe `../BUILDING.md`) und im lokalen Maven-Repo
installiert; danach:

```bash
cd bettersmp-suite
mvn clean install -DskipTests
```

Die fertigen Jars liegen anschließend in den jeweiligen `*/target/`-Ordnern
(und werden nach `../dist/` kopiert).

## Aufbau

```
bettersmp-suite/
├── compile-stubs/     Compile-Only-Stubs (Vault-Economy, PlaceholderAPI) – nie ausgeliefert
├── bettersmp/         BetterSMP
├── betterrtp/         BetterRTP
├── lifesteal-plus/    Lifesteal+
├── easybedrock/       EasyBedrock
└── fastshop/          FastShop
```

Jedes Plugin ist eigenständig – du kannst auch nur einzelne verwenden.
Lifesteal+ nutzt die CombatLog-API von BetterSMP, funktioniert aber auch ohne
BetterSMP (die Integration schaltet sich dann einfach ab).

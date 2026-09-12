# EasyBedrock

Bedrock-Crossplay in einem Schritt (Paper 1.21.11).

## Features
- **Auto-Installer** für **GeyserMC** und **Floodgate** von den offiziellen
  GeyserMC-Downloads (`download.geysermc.org`). Optional als Geyser-Erweiterungen:
  **GeyserConnect** und **Thunder** (per Config aktivierbar).
- **Ressourcenschonend**: Der Optimizer setzt die entscheidenden Geyser-Schlüssel
  auf **Direktverbindung** (`use-direct-connection: true`) und schaltet die
  doppelte Komprimierung ab. Dadurch geht ein Bedrock-Spieler **ohne zweite
  interne Verbindung** direkt auf den Server – er verbraucht etwa **so viele
  Ressourcen wie ein Java-Spieler**.
- **Versionssicher**: Der Optimizer patcht nur wenige, seit Jahren stabile
  Top-Level-Schlüssel per Text-Patch – Kommentare und alle übrigen Werte bleiben
  erhalten, nichts wird überschrieben.

## Befehle
- `/easybedrock status|install|optimize` – Permission `easybedrock.admin`

## Ablauf
1. Plugin einlegen, Server starten → Geyser + Floodgate werden geladen.
2. Server **einmal neu starten** → Geyser erzeugt seine Config.
3. EasyBedrock lädt **vor** Geyser (`loadbefore`) und patcht die Config; ggf.
   `/easybedrock optimize` ausführen. Danach ist die Direktverbindung aktiv.

> **Thunder**: Für Thunder ist im Config-Bereich `installer.thunder` bewusst kein
> Download voreingestellt, da keine offizielle Quelle verifiziert werden konnte.
> Trage dort `repo`/`asset` deiner Quelle ein und setze `enabled: true`.

Konfiguration: `config.yml` (Komponenten, Optimierung), `messages.yml`.

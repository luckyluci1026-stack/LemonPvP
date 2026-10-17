# Hinweise zu Lizenz und Herkunft

## PaperSwap ist eine Paper-basierte Fork – kein offizielles Paper-Projekt

PaperSwap ist eine eigenständige Fork von [Paper](https://github.com/PaperMC/Paper). Sie
steht in **keiner offiziellen Verbindung** zu PaperMC, Mojang Studios oder Microsoft.
„Paper“ ist ein Projekt von PaperMC, „Minecraft“ ist eine Marke von Mojang Studios.
Fehler in PaperSwap-Builds bitte **nicht** bei PaperMC melden, sondern in diesem Repository.

Die Server-JARs melden sich deshalb mit eigener Marke (`Brand-Id: paperswap:paperswap`,
`Brand-Name: PaperSwap`).

## Lizenzen

| Bestandteil | Lizenz | Quelle |
|---|---|---|
| Paper-Quellcode, der beim Build aus dem gepinnten Upstream-Commit bezogen wird | GPL-3.0 (Teile unter MIT, siehe `licenses/PAPER-LICENSE.md`) | https://github.com/PaperMC/Paper |
| Bukkit / CraftBukkit / Spigot-Anteile innerhalb von Paper | GPL-3.0 | über Paper |
| Eigene Fork-Patches (`versions/*/paperswap-*/…patch`), Runtime (`runtime/`), Skripte, Dokumentation | GPL-3.0 (`LICENSE`) | dieses Repository |
| Beispielmodul (`runtime/example-module`) | GPL-3.0 (`LICENSE`) | dieses Repository |

Ein ausgelieferter PaperSwap-Server ist ein abgeleitetes Werk von Paper und muss daher unter
der **GPL-3.0** weitergegeben werden (inkl. Quellcode-Angebot). Die Lizenztexte liegen in
`LICENSE` (GPL-3.0), `licenses/MIT.md` und `licenses/PAPER-LICENSE.md` (Paper-Lizenzhinweis
inkl. Liste der Autoren, die ihre Beiträge unter MIT gestellt haben).

> Offene Entscheidung für die Projektinhaber: Ob die Modul-API (`runtime-api`,
> `runtime-paper-api`) zusätzlich unter einer permissiven Lizenz (z. B. MIT) angeboten
> werden soll, damit Module unter beliebiger Lizenz entstehen können, ist eine
> rechtliche Entscheidung der Rechteinhaber und wurde bewusst **nicht** getroffen.

## Minecraft-Servercode

Dieses Repository enthält keinen Minecraft-Servercode. Paperweight lädt beim Build die
offiziellen Minecraft-Artefakte von Mojang; der erzeugte Paperclip-Launcher lädt beim
ersten Start die Vanilla-Server-JAR von Mojang und wendet die Patches lokal an. Wer einen
Server betreibt, muss die [Minecraft EULA](https://www.minecraft.net/eula) akzeptieren.

## Bereits vorhandene Dateien

`index.html` und `index.zip` im Repository-Wurzelverzeichnis existierten vor der Fork
(Website-Dateien) und sind nicht Teil der Fork. Für sie gelten die Bedingungen ihrer
Urheber.

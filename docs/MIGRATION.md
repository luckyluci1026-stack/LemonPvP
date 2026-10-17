# Von Lemon zu PaperSwap

Das Projekt hieß bis Commit `05a9c87` **Lemon**. Seit der Umbenennung heißt der Fork
**PaperSwap**. Dein Server **LemonPvP** und die **LemonPvP-Suite** (PvP-Module) behalten
ihren Namen.

## Was sich geändert hat

| Vorher (Lemon) | Jetzt (PaperSwap) | Läuft das Alte weiter? |
|---|---|---|
| Server-JAR `lemon-<mc>.jar`, Marke „Lemon“, Brand-Id `lemonpvp:lemon` | `paperswap-<mc>.jar`, „PaperSwap“, `paperswap:paperswap` | – (neue JAR verwenden) |
| `config/lemon-runtime.yml` | `config/paperswap.yml` | **ja**: Gibt es die neue Datei nicht, wird die alte gelesen (Warnung im Log) |
| Datenordner `lemon/` (Module, Daten, Patches, Audit-Log) | `paperswap/` | **ja**: Gibt es den neuen Ordner nicht, wird der alte benutzt (Warnung im Log) |
| `lemon-module.yml` in Modul-JARs | `paperswap-module.yml` oder `module.yml` | **ja**: alle drei Namen werden erkannt, aber nur einer pro JAR |
| `lemon-livepatch-agent.jar` | `paperswap-agent.jar` | **ja**: ein mit der alten JAR gestarteter Server wird erkannt (Hinweis im Status) |
| Patch-Dateien `.lemonpatch` | `.swappatch` | nein – alte Patches passen ohnehin nicht zur neuen Build-ID; neu bauen |
| Rechte `lemon.runtime.command`, `lemon.runtime.module.view|manage`, `lemon.runtime.live.view|update`, `lemon.runtime.command.reload` | `paperswap.command`, `paperswap.module.view|manage`, `paperswap.live.view|update`, `paperswap.command.reload` | nein – in LuckPerms o. ä. umbenennen |
| Java-Pakete `dev.lemonpvp.runtime.*` (Modul-API) | `dev.paperswap.runtime.*` | **nein** – Module gegen die neue API neu bauen (CI baut die mitgelieferten Module neu) |
| Gradle-Projekte `:lemon-server`, `:lemon-api` | `:paperswap-server`, `:paperswap-api` | – |
| Befehl `/fork` | `/fork` und neu `/paperswap` | ja |

Die LemonPvP-Suite behält ihr Paket `dev.lemonpvp.pvp.*`, ihre Namen und das Format von
`stats.tsv`; sie muss nur gegen die neue Modul-API neu gebaut werden.

## Umstellen eines bestehenden Servers

Server stoppen, dann im Serververzeichnis:

```bash
mv config/lemon-runtime.yml config/paperswap.yml
mv lemon paperswap
# In config/paperswap.yml Pfade anpassen, die in den alten Ordner zeigen, z. B.
#   module-directory: lemon/modules        ->  paperswap/modules
#   patch-directory:  lemon/patches/inbox  ->  paperswap/patches/inbox
```

Danach: neue Server-JAR (`paperswap-<mc>.jar`) und `paperswap-agent.jar` einsetzen
(Startbefehl: `-javaagent:paperswap-agent.jar`), Module durch die neu gebauten JARs
ersetzen (mit `.sha256`), Rechte umbenennen, Server starten. `/fork status` bzw.
`/paperswap status` zeigt, ob noch alte Pfade benutzt werden.

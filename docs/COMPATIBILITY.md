# Kompatibilität mit Serverrollen und Netzwerkkomponenten

PaperSwap ist für beliebige Serverrollen gedacht (Survival, Lobby, Minigames, Event-,
Entwicklungs- und Testserver). Die Fork setzt **kein** Netzwerk, keinen Proxy, keine
Datenbank und keine Client-Modifikation voraus und definiert kein eigenes Protokoll.

## Grundsatz

* Ohne Runtime (`enabled: false`, Standard) ist PaperSwap funktional Paper der jeweiligen
  Version plus drei Hook-Aufrufe, die nur eine Konfigurationsdatei lesen. Plugins, die mit
  dieser Paper-Version funktionieren, sollten daher auch mit PaperSwap funktionieren – das ist
  eine **Erwartung, kein Test**.
* Abweichung: `ServerBuildInfo#brandId()` liefert `paperswap:paperswap`, `brandName()` `PaperSwap`.
  Plugins, die explizit `isBrandCompatible(papermc:paper)` prüfen, sehen PaperSwap nicht als
  Paper (wie bei jeder umbenannten Fork).

## Netzwerkfunktionen als optionale Adapter

Die Runtime enthält **keine** Netzwerkadapter. Netzwerkfunktionen gehören in Module:

* Ein Adapter-Modul (z. B. `proxy-adapter`) stellt einen Service bereit
  (`ctx.services().provide(ProxyApi.class, ...)`, Fähigkeit `services`).
* Nutzer deklarieren es als **optionale** Abhängigkeit
  (`dependencies: [{id: proxy-adapter, optional: true}]`) und arbeiten ohne ihn weiter
  (`ctx.services().find(ProxyApi.class).orElse(...)`).
* Externe Bibliotheken (JDBC-Treiber, Redis-Clients) gehören in das Adapter-Modul und müssen
  dort in `onDisable` geschlossen werden. Ihre Abhängigkeit muss im Modul dokumentiert sein.

Das Verhalten bei fehlendem Adapter ist automatisiert getestet
(`ModuleLifecycleIntegrationTest.optionalNetworkAdapterMayBeAbsent`).

## Komponenten – Teststatus

| Komponente | Rolle | Status |
|---|---|---|
| Velocity / BungeeCord (Proxy-Modus, Forwarding) | Proxy | **nicht getestet**; PaperSwap ändert keine Proxy-Logik von Paper |
| ViaVersion / ViaBackwards | Protokollübersetzung (Plugin) | **nicht getestet**; Plugin-Kompatibilität hängt von der Paper-Version ab |
| Geyser / Floodgate | Bedrock-Brücke (Plugin oder Proxy) | **nicht getestet** |
| LuckPerms | Berechtigungen (Plugin) | **nicht getestet**; PaperSwap-Berechtigungen sind normale Bukkit-Permissions |
| MariaDB / MySQL / Redis | Datenhaltung | keine Abhängigkeit; nur über Module |
| spark (gebündelt in Paper) | Profiler | unverändert aus Paper übernommen |

Kritische Kombinationen sollten vor dem Produktiveinsatz auf einem Testserver mit den
CI-Artefakten geprüft werden (Startreihenfolge: Proxy → PaperSwap-Server mit den Plugins).

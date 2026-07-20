# BetterRTP

Vollständig asynchrones, sicheres Random-Teleport-Plugin (Paper 1.21.11).

## Highlights
- **Asynchrone Positionssuche**: Chunk-Load async → `ChunkSnapshot` →
  Sicherheits-Scan im ForkJoinPool → Teleport im Main-Thread. Der Main-Thread
  wird nie durch Block-Iteration blockiert (Ziel: konstante 20 TPS).
- **Sichere Landung**: kein Lava/Wasser/Kaktus/Feuer usw., 2 Blöcke Kopffreiheit,
  eigenes Nether-Deckenscan (kein Bedrock-Dach).
- **Welt-Profile**: pro Welt Radius/Zentrum, Biom-Blacklist, Weltborder wird respektiert.
- **Cooldown & Warmup**: Warmup bricht bei Bewegung/Schaden ab, Actionbar-Countdown,
  kurze Unverwundbarkeit nach der Ankunft.
- **Optionale Kosten** über Vault/EssentialsX-Economy.

## Befehle
- `/rtp [Welt]` (Aliase `/wild`, `/randomtp`) – Permission `betterrtp.use` (Standard: alle)
- `/betterrtp reload|version` – `betterrtp.admin`

## Bypass-Permissions
`betterrtp.bypass.cooldown`, `betterrtp.bypass.warmup`, `betterrtp.bypass.cost`,
`betterrtp.world` (RTP in beliebiger Welt).

Konfiguration: `config.yml` (Profile, Cooldown/Warmup, Economy), `messages.yml`.

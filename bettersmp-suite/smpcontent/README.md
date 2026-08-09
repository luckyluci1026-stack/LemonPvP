# SMPContent

Eigene **Blöcke** und **Items** passend zum SMP-Texturepack (Paper 1.21.11).

## Inhalt

**8 Blöcke:** Rubinerz, Saphirerz, Rubinblock, Saphirblock, Marmor,
Dunkler Marmor, Neonlampe, Münzhaufen

**7 Items:** Rubin, Saphir, Magischer Staub, Shop-Gutschein, Schlüssel,
Rubinschwert, Rubinspitzhacke

(Dazu kommen Herz, Revive-Totem und Münze aus Lifesteal+ / dem Pack.)

## Wie komme ich an die Sachen?

**1. Craften** – jedes Teil hat ein Rezept (alle in der `config.yml` änderbar):

| Ergebnis | Rezept |
|---|---|
| Rubin | 4× Redstone + 1× Diamant (Kreuz) |
| Saphir | 4× Lapis + 1× Diamant (Kreuz) |
| Rubinblock / Saphirblock | 9× Rubin bzw. Saphir |
| Rubinerz / Saphirerz | 8× Stein + 1× Rubin/Saphir |
| Marmor (8×) | 8× Glatter Stein + 1× Quarz |
| Dunkler Marmor (8×) | 8× Blackstone + 1× Quarz |
| Neonlampe | 4× Glowstone-Staub + 1× Seelaterne |
| Münzhaufen | 9× Goldbarren |
| Magischer Staub (3×) | Lohenstaub + Glowstone-Staub + Redstone |
| Shop-Gutschein | 2× Papier + 1× Goldbarren |
| Schlüssel | 2× Goldbarren + 1× Eisenbarren |
| Rubinschwert | 2× Rubin + 1× Stock |
| Rubinspitzhacke | 3× Rubin + 2× Stöcke |

**2. `/smpcontent list`** – GUI mit allen Inhalten. Klick = 1 Stück,
Shift-Klick = 64 Stück. (Recht `smpcontent.admin`)

**3. `/smpcontent give <Spieler> <Id> [Menge]`** – gezielt vergeben.

## Wie die Blöcke funktionieren

Minecraft erlaubt keine echten neuen Block-IDs über ein Resource-Pack. Die
gängige Lösung (die auch große Content-Plugins nutzen) sind **Note-Block-
Zustände**: Jeder eigene Block ist ein Notenblock mit einer festen
Instrument/Noten-Kombination, und das Texturepack lenkt genau diese
Kombination auf ein eigenes Modell.

Damit der Zustand hält, unterdrückt das Plugin für **genau diese Blöcke** drei
Vanilla-Verhalten: Instrumentwechsel durch den Block darunter, Umstimmen per
Rechtsklick und den Notenklang. **Normale Notenblöcke bleiben unberührt** –
sie funktionieren wie immer.

> Die Zustände in der `config.yml` müssen exakt zu
> `texturepack/dist/blocks-states.yml` passen. Änderst du einen, ändere beide.

## Wichtig

Ohne installiertes Texturepack funktionieren alle Blöcke und Items ganz
normal – sie sehen dann nur aus wie ihr Basis-Item (Notenblock,
Amethystsplitter …). Das Pack liefert nur das Aussehen.

## Befehle & Rechte

- `/smpcontent give|list|reload` – Recht `smpcontent.admin` (Standard: OP)
- `smpcontent.place` – darf eigene Blöcke setzen (Standard: alle)

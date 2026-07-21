# docs.lemonpvp.de

A single self-contained documentation page explaining the LemonPvP **Emote
System** — how 3D cosmetics and Fortnite-style emotes run on vanilla Minecraft
with no mods and no resource pack.

## Deploy

It's one static file with no build step and no dependencies:

1. Upload `index.html` (and `LICENSE`) to the document root of `docs.lemonpvp.de`.
2. Done. It pulls Poppins + Inter from Google Fonts and needs nothing else.

No PHP, no database, no credentials — it is pure HTML/CSS/JS and works on any
static host (nginx, Caddy, GitHub Pages, Cloudflare Pages, …).

## What's inside

- The five packet techniques: spawn, transform + quaternion math, player-head
  textures, the no-lag movement tracker, and client-side interpolation.
- The dance-emote layer (armour-stand body double + keyframed poses).
- A Display/armour-stand metadata index reference and the command list.

## Not included

By design, the page shows only the emote/cosmetic layer — never the network's
moderation, economy, matchmaking or database code, and never any server
address, token or credential.

## License

GNU General Public License v3.0 — see `LICENSE`. Covers this documentation
and its reference snippets; anything derived from them must stay open source
under the same terms.


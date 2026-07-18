# LemonPvP HTTP API — Guide

The API is built into **LemonCore** (`HttpApiManager`) — every backend server can
expose its own instance. It is plain **REST + JSON over HTTP** with a **Bearer
token**. No MCP, no SDK needed: `curl`, a browser, your website's JavaScript, a
Discord bot — anything that can send HTTP works.

> **Why REST and not MCP?** MCP (Model Context Protocol) is a protocol for AI
> assistants (like Claude) to call tools — it is NOT meant for websites or
> status pages, and browsers can't speak it. The right architecture is: a plain
> REST API on the network + your web pages fetch from it. If you later want
> Claude to manage the server directly, you can wrap this same REST API in a
> tiny MCP server (e.g. with the official MCP SDK) that just forwards calls —
> the REST API stays the single source of truth. REST first, MCP as an optional
> layer on top.

## 1. Enabling the API

In `plugins/LemonCore/config.yml` on each server:

```yaml
http-api:
  enabled: true
  port: 8080            # pick a unique port per server on the same machine
  bind: 127.0.0.1       # NEVER bind 0.0.0.0 — expose via reverse proxy only
  key: "<GENERATE A LONG RANDOM TOKEN>"   # e.g.: openssl rand -hex 32
```

Restart the server. The log shows `[HttpAPI] Started on 127.0.0.1:8080`.

## 2. Authentication

Every endpoint **except `/api/status`** requires the token:

```
Authorization: Bearer <your token>
```

Missing/wrong token → `401 {"error":"Unauthorized"}`. Keep the token secret —
whoever has it can ban players and run console commands. Use a different token
per server, rotate them if leaked (`config.yml` → restart).

## 3. Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/status` | — | Health: players, TPS, uptime, version (for the status page) |
| GET | `/api/player/{name}` | ✔ | Player profile: stats, coins, rank |
| POST | `/api/player/{name}/ban` | ✔ | Ban (`{"reason":"...","duration":"7d"}`) |
| POST | `/api/player/{name}/unban` | ✔ | Unban |
| POST | `/api/player/{name}/mute` | ✔ | Mute (same body as ban) |
| POST | `/api/player/{name}/unmute` | ✔ | Unmute |
| POST | `/api/player/{name}/coins` | ✔ | Adjust coins (`{"action":"add","amount":100}`) |
| GET | `/api/server/stats` | ✔ | Server stats (detailed) |
| POST | `/api/console` | ✔ | Run a console command (`{"command":"..."}`) |
| GET | `/api/leaderboard?stat=kills\|coins&limit=10` | ✔ | Top players |
| GET | `/api/tournaments` | ✔ | All tournaments with state/gamemode |
| GET | `/api/history/{name}` | ✔ | Ban + mute history (last 25 each) |
| POST | `/api/broadcast` | ✔ | Broadcast MiniMessage (`{"message":"<green>Hi"}`) |

### Examples

```bash
TOKEN="your-token-here"
BASE="https://api.lemonpvp.de/lobby"

curl -s $BASE/api/status                                        # public
curl -s -H "Authorization: Bearer $TOKEN" $BASE/api/player/Notch
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/leaderboard?stat=kills&limit=10"
curl -s -H "Authorization: Bearer $TOKEN" -X POST $BASE/api/broadcast \
     -H "Content-Type: application/json" -d '{"message":"<gradient:#fffb00:#00ff00>Restart in 5min!"}'
```

## 4. Exposing it safely (reverse proxy + HTTPS)

The API binds to localhost only. On your webserver machine (or via a tunnel to
the game machine), put nginx/Caddy in front for TLS. Example nginx:

```nginx
# api.lemonpvp.de
location /lobby/ { proxy_pass http://10.0.0.2:8080/; }   # lobby backend
location /duels1/ { proxy_pass http://10.0.0.3:8080/; }  # duels-01 backend
```

For the PUBLIC status page, expose **only** `/api/status` without auth:

```nginx
location /lobby/api/status { proxy_pass http://10.0.0.2:8080/api/status; }
# and do NOT expose the other paths publicly — or protect them:
location /lobby/api/ { proxy_pass http://10.0.0.2:8080/api/; allow 127.0.0.1; deny all; }
```

Add CORS for the status page's origin if it's on another domain:
`add_header Access-Control-Allow-Origin https://status.lemonpvp.de;`

## 5. The status page (status.lemonpvp.de)

`web/status.html` is a self-contained Discord/GitHub-style status page:
upload it to your webserver as the document root of `status.lemonpvp.de`,
then edit the `SERVERS` array at the top of the file — one entry per backend
with its public `/api/status` URL. It polls every 30 s, shows
Operational / Down per server, player counts, TPS and uptime, and a
green/yellow/red banner exactly like the big status pages.

## 6. Storing stats in your own database via the API

Everything the API serves already lives in the shared MySQL database
(`lc_players`, `lc_elo`, `lp_duel_records`, …). For your website you have two
clean options:

1. **Pull via API** (recommended): your webserver periodically calls
   `/api/leaderboard`, `/api/player/{name}` etc. and caches the JSON — no DB
   credentials ever leave the network.
2. **Read-only DB user**: create a MySQL user with `SELECT`-only rights for
   the website. More power, more risk — prefer option 1 unless you need
   complex queries.

Never put the Bearer token or DB credentials in client-side JavaScript — only
in server-side code on your webserver. The status page needs neither (it only
calls the public `/api/status`).

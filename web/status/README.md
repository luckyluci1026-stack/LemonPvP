# status.lemonpvp.de — Deployment

A Discord/GitHub-style status page with 90-day uptime bars. Two files:

- **`index.html`** — the page visitors see. Only ever calls `api.php` on the
  same domain; it contains no tokens and no backend URLs.
- **`api.php`** — the server-side aggregator. Holds the backend list (and the
  Bearer token, if you protect `/api/status`), polls every backend, records
  daily uptime samples into `history.json`, caches responses for 30 s.

## Setup (any PHP-capable webhosting)

1. Upload this folder as the document root of `status.lemonpvp.de`
   (needs PHP 8+ with curl — standard everywhere).
2. Edit the CONFIG block at the top of `api.php`:
   - `$SERVERS` — one entry per backend with its `/api/status` URL
     (the LemonCore HTTP API, ideally with native HTTPS enabled — see `web/API.md`).
   - `$TOKEN` — leave empty; only set it if you protected `/api/status`.
3. Make the folder writable for PHP so `history.json` / `cache.json` can be
   created (usually automatic; otherwise `chown www-data` or `chmod 775 .`).
4. Open the page. Bars start gray and fill green day by day as history builds.

## Notes

- Uptime % and bars come from `history.json` — daily ok/total counters kept
  for 120 days, shown for 90. Back it up if the numbers matter to you.
- The 30 s cache in `api.php` means heavy traffic on the status page causes
  at most 2 requests/minute per backend.
- Never put the API token into `index.html` — anything in the HTML/JS is
  public. That's exactly why `api.php` exists.

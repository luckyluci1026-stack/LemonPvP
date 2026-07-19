<?php
/**
 * status.lemonpvp.de — server-side aggregator.
 *
 * The page (index.html) only ever talks to THIS file; the Bearer token and the
 * backend URLs stay server-side and are never exposed to visitors. Each call
 * polls every backend's /api/status (cached 30s so refreshes are cheap),
 * records an uptime sample per day into history.json (feeds the 90-day bars),
 * and returns one aggregated JSON document.
 *
 * Deploy: upload this folder to the docroot of status.lemonpvp.de, fill in
 * $SERVERS (+ $TOKEN if you protect /api/status), make sure PHP can write
 * history.json/cache.json in this directory (chmod 775 or chown www-data).
 */

// ── CONFIG ──────────────────────────────────────────────────────────────────
// Bearer token for the LemonCore API. The default /api/status endpoint is
// PUBLIC, so leave this empty unless you protected it behind the token too.
$TOKEN = '';

// One entry per backend: display name + its /api/status URL.
$SERVERS = [
    ['name' => 'Lobby',    'url' => 'https://api.lemonpvp.de:8081/api/status'],
    ['name' => 'Duels-01', 'url' => 'https://api.lemonpvp.de:8082/api/status'],
    ['name' => 'Duels-02', 'url' => 'https://api.lemonpvp.de:8083/api/status'],
    ['name' => 'Events',   'url' => 'https://api.lemonpvp.de:8084/api/status'],
];

$HISTORY_DAYS  = 120;  // stored; the page shows the last 90
$CACHE_SECONDS = 30;
// ────────────────────────────────────────────────────────────────────────────

$HISTORY_FILE = __DIR__ . '/history.json';
$CACHE_FILE   = __DIR__ . '/cache.json';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

// Serve the cached aggregate if it is still fresh.
if (file_exists($CACHE_FILE) && time() - filemtime($CACHE_FILE) < $CACHE_SECONDS) {
    readfile($CACHE_FILE);
    exit;
}

function fetch_status(string $url, string $token): ?array {
    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 3,
        CURLOPT_TIMEOUT        => 5,
        CURLOPT_HTTPHEADER     => $token !== '' ? ['Authorization: Bearer ' . $token] : [],
    ]);
    $body = curl_exec($ch);
    $code = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    curl_close($ch);
    if ($body === false || $code !== 200) return null;
    $json = json_decode($body, true);
    return is_array($json) ? $json : null;
}

$history = [];
if (file_exists($HISTORY_FILE)) {
    $decoded = json_decode((string) file_get_contents($HISTORY_FILE), true);
    if (is_array($decoded)) $history = $decoded;
}

$today = gmdate('Y-m-d');
$out = ['generatedAt' => gmdate('c'), 'servers' => []];

foreach ($SERVERS as $server) {
    $name   = $server['name'];
    $status = fetch_status($server['url'], $TOKEN);
    $up     = $status !== null;

    // Record today's sample.
    if (!isset($history[$name])) $history[$name] = [];
    if (!isset($history[$name][$today])) $history[$name][$today] = ['ok' => 0, 'total' => 0];
    $history[$name][$today]['total']++;
    if ($up) $history[$name][$today]['ok']++;

    // Trim history to the retention window.
    if (count($history[$name]) > $HISTORY_DAYS) {
        ksort($history[$name]);
        $history[$name] = array_slice($history[$name], -$HISTORY_DAYS, null, true);
    }

    // Build the last-90-days series (null = no data for that day).
    $days = [];
    for ($i = 89; $i >= 0; $i--) {
        $date = gmdate('Y-m-d', time() - $i * 86400);
        if (isset($history[$name][$date]) && $history[$name][$date]['total'] > 0) {
            $entry  = $history[$name][$date];
            $days[] = round($entry['ok'] / $entry['total'], 4);
        } else {
            $days[] = null;
        }
    }
    $known = array_filter($days, fn($d) => $d !== null);
    $uptimePct = count($known) > 0 ? round(array_sum($known) / count($known) * 100, 2) : null;

    $out['servers'][] = [
        'name'      => $name,
        'up'        => $up,
        'players'   => $up ? ($status['players'] ?? null) : null,
        'maxPlayers'=> $up ? ($status['maxPlayers'] ?? null) : null,
        'tps'       => $up ? ($status['tps'] ?? null) : null,
        'uptimeSeconds' => $up ? ($status['uptimeSeconds'] ?? null) : null,
        'version'   => $up ? ($status['version'] ?? null) : null,
        'days'      => $days,
        'uptimePct' => $uptimePct,
    ];
}

@file_put_contents($HISTORY_FILE, json_encode($history));
$payload = json_encode($out);
@file_put_contents($CACHE_FILE, $payload);
echo $payload;

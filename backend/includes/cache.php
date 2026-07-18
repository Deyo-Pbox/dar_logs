<?php
/**
 * DAR Activity Logs - Legacy procedural cache wrapper
 *
 * Delegates to DarLogs\Core\Cache when available, falls back to file-based impl otherwise.
 */

if (class_exists(\DarLogs\Core\Cache::class, false)) {
    function darCacheGet(string $key, mixed $default = null): mixed {
        return \DarLogs\Core\Cache::get($key, $default);
    }

    function darCacheSet(string $key, mixed $data, int $ttlSeconds = 60): void {
        \DarLogs\Core\Cache::set($key, $data, $ttlSeconds);
    }

    function darCacheForget(string $key): void {
        \DarLogs\Core\Cache::forget($key);
    }
} else {
    function darCacheGet(string $key, mixed $default = null): mixed
    {
        $cacheDir = __DIR__ . '/../storage/cache';
        $path = $cacheDir . '/' . preg_replace('/[^a-zA-Z0-9._-]/', '_', $key) . '.cache';

        if (!file_exists($path)) return $default;
        $raw = @file_get_contents($path);
        if ($raw === false) return $default;

        $entry = @json_decode($raw, true);
        if (!is_array($entry) || !isset($entry['expires'], $entry['data'])) return $default;

        if ($entry['expires'] !== 0 && $entry['expires'] < time()) {
            @unlink($path);
            return $default;
        }
        return $entry['data'];
    }

    function darCacheSet(string $key, mixed $data, int $ttlSeconds = 60): void
    {
        $cacheDir = __DIR__ . '/../storage/cache';
        if (!is_dir($cacheDir)) mkdir($cacheDir, 0755, true);

        $path = $cacheDir . '/' . preg_replace('/[^a-zA-Z0-9._-]/', '_', $key) . '.cache';
        $entry = ['expires' => $ttlSeconds > 0 ? time() + $ttlSeconds : 0, 'data' => $data];
        $tmp = $path . '.' . bin2hex(random_bytes(6)) . '.tmp';
        file_put_contents($tmp, json_encode($entry, JSON_UNESCAPED_SLASHES), LOCK_EX);
        rename($tmp, $path);
    }

    function darCacheForget(string $key): void
    {
        $path = __DIR__ . '/../storage/cache/' . preg_replace('/[^a-zA-Z0-9._-]/', '_', $key) . '.cache';
        @unlink($path);
    }
}

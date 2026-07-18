<?php

declare(strict_types=1);

namespace DarLogs\Core;

class Cache
{
    private static string $cacheDir = '';
    private static bool $apcuAvailable;

    public static function init(string $cacheDir): void
    {
        self::$cacheDir = rtrim($cacheDir, '/\\') . '/';
        if (!is_dir(self::$cacheDir)) {
            mkdir(self::$cacheDir, 0755, true);
        }
        self::$apcuAvailable = extension_loaded('apcu') && apcu_enabled();
    }

    public static function get(string $key, mixed $default = null): mixed
    {
        if (self::$apcuAvailable) {
            $entry = apcu_fetch('dar_' . $key, $success);
            if ($success && isset($entry['expires'], $entry['data'])) {
                if ($entry['expires'] === 0 || $entry['expires'] >= time()) {
                    return $entry['data'];
                }
            }
            return $default;
        }

        $path = self::$cacheDir . self::safeFilename($key) . '.cache';
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

    public static function set(string $key, mixed $data, int $ttlSeconds = 60): void
    {
        $entry = [
            'expires' => $ttlSeconds > 0 ? time() + $ttlSeconds : 0,
            'data'    => $data,
        ];

        if (self::$apcuAvailable) {
            apcu_store('dar_' . $key, $entry, $ttlSeconds > 0 ? $ttlSeconds : 0);
            return;
        }

        $path = self::$cacheDir . self::safeFilename($key) . '.cache';
        $tmp = $path . '.' . bin2hex(random_bytes(6)) . '.tmp';
        file_put_contents($tmp, json_encode($entry, JSON_UNESCAPED_SLASHES), LOCK_EX);
        rename($tmp, $path);
    }

    public static function forget(string $key): void
    {
        if (self::$apcuAvailable) {
            apcu_delete('dar_' . $key);
            return;
        }

        $path = self::$cacheDir . self::safeFilename($key) . '.cache';
        @unlink($path);
    }

    public static function flush(string $prefix = ''): void
    {
        if (self::$apcuAvailable) {
            if ($prefix !== '') {
                $info = apcu_cache_info();
                foreach ($info['cache_list'] ?? [] as $entry) {
                    $info_key = $entry['info'] ?? $entry['key'] ?? '';
                    if (str_starts_with($info_key, 'dar_' . $prefix)) {
                        apcu_delete($info_key);
                    }
                }
            } else {
                apcu_clear_cache();
            }
            return;
        }

        $files = glob(self::$cacheDir . ($prefix !== '' ? self::safeFilename($prefix) . '*' : '*.cache'));
        if ($files) {
            foreach ($files as $file) {
                @unlink($file);
            }
        }
    }

    public static function remember(string $key, int $ttlSeconds, callable $fn): mixed
    {
        $value = self::get($key, $dummy = new \stdClass());
        if ($value !== $dummy) return $value;

        $value = $fn();
        self::set($key, $value, $ttlSeconds);
        return $value;
    }

    private static function safeFilename(string $key): string
    {
        return preg_replace('/[^a-zA-Z0-9._-]/', '_', $key);
    }
}

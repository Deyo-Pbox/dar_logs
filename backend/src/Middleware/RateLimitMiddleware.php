<?php

declare(strict_types=1);

namespace DarLogs\Middleware;

use DarLogs\Core\Middleware;
use DarLogs\Core\Request;
use DarLogs\Core\Response;

class RateLimitMiddleware implements Middleware
{
    private int $maxAttempts;
    private int $decaySeconds;

    public function __construct(int $maxAttempts = 10, int $decaySeconds = 60)
    {
        $this->maxAttempts = $maxAttempts;
        $this->decaySeconds = $decaySeconds;
    }

    public function handle(Request $request, callable $next): Response
    {
        $ip = $_SERVER['HTTP_X_FORWARDED_FOR']
            ?? $_SERVER['HTTP_X_REAL_IP']
            ?? $_SERVER['REMOTE_ADDR']
            ?? '127.0.0.1';
        $ip = str_contains($ip, ',') ? explode(',', $ip)[0] : $ip;
        $key = 'ratelimit_' . md5($ip . '_' . ($request->getPath() ?? ''));

        $attempts = $this->getAttempts($key);

        if ($attempts >= $this->maxAttempts) {
            return Response::error(429, 'Too many requests. Please try again later.');
        }

        $this->increment($key);
        return $next($request);
    }

    private function getAttempts(string $key): int
    {
        if (extension_loaded('apcu') && apcu_enabled()) {
            return (int) apcu_fetch($key, $success);
        }

        $path = $this->cachePath($key);
        if (!file_exists($path)) return 0;

        $data = @json_decode(@file_get_contents($path) ?: '', true);
        if (!is_array($data) || !isset($data['hits'], $data['expires'])) return 0;
        if ($data['expires'] < time()) {
            @unlink($path);
            return 0;
        }
        return (int) $data['hits'];
    }

    private function increment(string $key): void
    {
        $hits = $this->getAttempts($key) + 1;
        $entry = [
            'hits'    => $hits,
            'expires' => time() + $this->decaySeconds,
        ];

        if (extension_loaded('apcu') && apcu_enabled()) {
            apcu_store($key, $entry, $this->decaySeconds);
            return;
        }

        $path = $this->cachePath($key);
        $dir = dirname($path);
        if (!is_dir($dir)) {
            @mkdir($dir, 0755, true);
        }
        if (is_dir($dir)) {
            @file_put_contents($path, json_encode($entry), LOCK_EX);
        }
    }

    private function cachePath(string $key): string
    {
        $cacheDir = __DIR__ . '/../../storage/cache';
        return $cacheDir . '/' . preg_replace('/[^a-zA-Z0-9._-]/', '_', $key) . '.json';
    }
}

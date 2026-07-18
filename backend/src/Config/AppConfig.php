<?php

declare(strict_types=1);

namespace DarLogs\Config;

use Dotenv\Dotenv;

class AppConfig
{
    private static bool $loaded = false;

    public static function load(string $basePath): void
    {
        if (self::$loaded) {
            return;
        }
        self::$loaded = true;

        if (file_exists($basePath . '/.env')) {
            $dotenv = Dotenv::createImmutable($basePath);
            $dotenv->load();
        }

        $required = ['DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASS', 'JWT_SECRET'];
        foreach ($required as $key) {
            if (!isset($_ENV[$key]) || $_ENV[$key] === null) {
                throw new \RuntimeException("Missing required environment variable: {$key}");
            }
            if ($key !== 'DB_PASS' && empty($_ENV[$key])) {
                throw new \RuntimeException("Required environment variable is empty: {$key}");
            }
        }
    }

    public static function get(string $key, mixed $default = null): mixed
    {
        return $_ENV[$key] ?? $default;
    }

    public static function isProduction(): bool
    {
        return (self::get('APP_ENV', 'production')) === 'production';
    }

    public static function isDebug(): bool
    {
        return filter_var(self::get('APP_DEBUG', 'false'), FILTER_VALIDATE_BOOLEAN);
    }

    public static function jwtSecret(): string
    {
        return (string) self::get('JWT_SECRET');
    }

    public static function jwtExpiry(): int
    {
        return (int) self::get('JWT_EXPIRY', 86400);
    }
}

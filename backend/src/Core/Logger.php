<?php

declare(strict_types=1);

namespace DarLogs\Core;

class Logger
{
    private static string $logDir = '';

    private const LEVELS = [
        'DEBUG'     => 0,
        'INFO'      => 1,
        'WARNING'   => 2,
        'ERROR'     => 3,
    ];

    private static string $minLevel = 'INFO';

    public static function init(string $logDir): void
    {
        self::$logDir = rtrim($logDir, '/\\') . '/';
        if (!is_dir(self::$logDir)) {
            mkdir(self::$logDir, 0755, true);
        }
    }

    public static function debug(string $message, array $context = []): void
    {
        self::log('DEBUG', $message, $context);
    }

    public static function info(string $message, array $context = []): void
    {
        self::log('INFO', $message, $context);
    }

    public static function warning(string $message, array $context = []): void
    {
        self::log('WARNING', $message, $context);
    }

    public static function error(string $message, array $context = []): void
    {
        self::log('ERROR', $message, $context);
    }

    private static function log(string $level, string $message, array $context): void
    {
        if ((self::LEVELS[$level] ?? 0) < (self::LEVELS[self::$minLevel] ?? 0)) {
            return;
        }

        $entry = [
            'timestamp' => date('c'),
            'level'     => $level,
            'message'   => $message,
            'ip'        => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'uri'       => $_SERVER['REQUEST_URI'] ?? 'cli',
            'context'   => $context,
        ];

        $line = json_encode($entry, JSON_UNESCAPED_SLASHES);

        if (self::$logDir === '') {
            error_log($line);
            return;
        }

        $file = self::$logDir . 'app-' . date('Y-m-d') . '.log';
        file_put_contents($file, $line . "\n", FILE_APPEND | LOCK_EX);
    }
}

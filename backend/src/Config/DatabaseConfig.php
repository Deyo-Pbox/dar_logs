<?php

declare(strict_types=1);

namespace DarLogs\Config;

use PDO;
use PDOException;

class DatabaseConfig
{
    private static ?string $dsnCache = null;
    private static ?string $userCache = null;
    private static ?string $passCache = null;

    public static function createConnection(): PDO
    {
        if (self::$dsnCache === null) {
            $host    = AppConfig::get('DB_HOST', 'localhost');
            $port    = AppConfig::get('DB_PORT', '3306');
            $name    = AppConfig::get('DB_NAME', 'dar_logs');
            $user    = AppConfig::get('DB_USER', 'root');
            $pass    = AppConfig::get('DB_PASS', '');
            $charset = AppConfig::get('DB_CHARSET', 'utf8mb4');

            self::$dsnCache  = "mysql:host={$host};port={$port};dbname={$name};charset={$charset}";
            self::$userCache = $user;
            self::$passCache = $pass;
        }

        try {
            $pdo = new PDO(self::$dsnCache, self::$userCache, self::$passCache, [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ]);

            $pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");

            return $pdo;
        } catch (PDOException $e) {
            throw new \RuntimeException('Database connection failed: ' . $e->getMessage(), 500, $e);
        }
    }
}

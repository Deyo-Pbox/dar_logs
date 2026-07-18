<?php

declare(strict_types=1);

namespace DarLogs\Core;

use DarLogs\Config\DatabaseConfig;
use PDO;

class Database
{
    private static ?PDO $instance = null;

    public static function getInstance(): PDO
    {
        if (self::$instance === null) {
            self::$instance = DatabaseConfig::createConnection();
        }
        return self::$instance;
    }
}

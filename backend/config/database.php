<?php
/**
 * DAR Activity Logs - Database Configuration
 * MySQL connection via PDO — reads from environment for InfinityFree compatibility
 */

$envVars = [];
$envFile = __DIR__ . '/../.env';
if (file_exists($envFile)) {
    foreach (file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
        $line = trim($line);
        if ($line === '' || str_starts_with($line, '#')) continue;
        if (str_contains($line, '=')) {
            [$key, $value] = explode('=', $line, 2);
            $envVars[trim($key)] = trim($value);
        }
    }
}

define('DB_HOST', getenv('DB_HOST') ?: ($envVars['DB_HOST'] ?? 'localhost'));
define('DB_PORT', getenv('DB_PORT') ?: ($envVars['DB_PORT'] ?? '3306'));
define('DB_NAME', getenv('DB_NAME') ?: ($envVars['DB_NAME'] ?? 'dar_logs'));
define('DB_USER', getenv('DB_USER') ?: ($envVars['DB_USER'] ?? 'root'));
define('DB_PASS', getenv('DB_PASS') ?: ($envVars['DB_PASS'] ?? ''));
define('DB_CHARSET', 'utf8mb4');

function getDB(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $dsn = 'mysql:host=' . DB_HOST . ';port=' . DB_PORT . ';dbname=' . DB_NAME . ';charset=' . DB_CHARSET;
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];
        $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        $pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
    }
    return $pdo;
}

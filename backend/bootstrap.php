<?php

declare(strict_types=1);

use DarLogs\Config\AppConfig;
use DarLogs\Exceptions\AppException;
use DarLogs\Exceptions\ValidationException;

$basePath = __DIR__;
require_once $basePath . '/vendor/autoload.php';

AppConfig::load($basePath);

if (AppConfig::isDebug()) {
    error_reporting(E_ALL);
    ini_set('display_errors', '1');
} else {
    error_reporting(0);
    ini_set('display_errors', '0');
}

set_exception_handler(function (Throwable $e): void {
    if ($e instanceof AppException) {
        $status = $e->getHttpCode();
        $message = $e->getMessage();
        $errors = ($e instanceof ValidationException) ? $e->getErrors() : null;
    } elseif ($e instanceof \PDOException) {
        \DarLogs\Core\Logger::error('Database error', ['error' => $e->getMessage()]);
        $status = 500;
        $message = AppConfig::isDebug() ? $e->getMessage() : 'Database error';
        $errors = null;
    } else {
        \DarLogs\Core\Logger::error(get_class($e) . ': ' . $e->getMessage(), [
            'file' => $e->getFile(),
            'line' => $e->getLine(),
        ]);
        $status = 500;
        $message = AppConfig::isDebug() ? $e->getMessage() : 'Internal server error';
        $errors = null;
    }

    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array_filter([
        'success' => false,
        'message' => $message,
        'errors'  => $errors,
    ], fn($v) => $v !== null), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
});

header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');

\DarLogs\Core\Cache::init($basePath . '/storage/cache');
\DarLogs\Core\Logger::init($basePath . '/storage/logs');

<?php

declare(strict_types=1);

$baseDir = __DIR__;
if (!file_exists($baseDir . '/bootstrap.php')) {
    $baseDir = dirname(__DIR__);
}

require_once $baseDir . '/bootstrap.php';

use DarLogs\Core\Router;
use DarLogs\Core\Request;

$request = new Request();
$router = new Router();

$loadRoutes = require $baseDir . '/routes/api.php';
$loadRoutes($router);

// Serve static frontend files for non-API requests
$path = $request->getPath();
if (!str_starts_with($path, '/api/')) {
    $publicPath = __DIR__ . $path;
    $extension = pathinfo($path, PATHINFO_EXTENSION);

    if ($path === '/' || $path === '' || ($extension === '' && !file_exists($publicPath))) {
        readfile(__DIR__ . '/index.html');
        exit;
    }

    if (file_exists($publicPath) && is_file($publicPath)) {
        $mimeTypes = [
            'html' => 'text/html',
            'css'  => 'text/css',
            'js'   => 'application/javascript',
            'json' => 'application/json',
            'png'  => 'image/png',
            'jpg'  => 'image/jpeg',
            'svg'  => 'image/svg+xml',
            'ico'  => 'image/x-icon',
            'woff' => 'font/woff',
            'woff2'=> 'font/woff2',
        ];
        $mime = $mimeTypes[$extension] ?? 'application/octet-stream';
        header('Content-Type: ' . $mime);
        readfile($publicPath);
        exit;
    }

    // Fallback to SPA shell for hash routes
    readfile(__DIR__ . '/index.html');
    exit;
}

$response = $router->dispatch($request);
$response->send();

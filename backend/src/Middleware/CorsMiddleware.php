<?php

declare(strict_types=1);

namespace DarLogs\Middleware;

use DarLogs\Config\AppConfig;
use DarLogs\Core\Middleware;
use DarLogs\Core\Request;
use DarLogs\Core\Response;

class CorsMiddleware implements Middleware
{
    public function handle(Request $request, callable $next): Response
    {
        $origin = $this->getAllowedOrigin($request);

        header('Access-Control-Allow-Origin: ' . $origin);
        header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type, Authorization');
        header('Access-Control-Allow-Credentials: true');
        header('Access-Control-Max-Age: 86400');

        if ($request->getMethod() === 'OPTIONS') {
            return Response::ok();
        }

        return $next($request);
    }

    private function getAllowedOrigin(Request $request): string
    {
        $allowed = AppConfig::get('CORS_ORIGIN', '');
        if ($allowed === '' || $allowed === '*') {
            $referer = $_SERVER['HTTP_ORIGIN'] ?? $_SERVER['HTTP_REFERER'] ?? '';
            if ($referer !== '') {
                return $referer;
            }
            return '*';
        }

        $origins = array_map('trim', explode(',', $allowed));
        $incoming = $_SERVER['HTTP_ORIGIN'] ?? '';

        if (in_array($incoming, $origins, true)) {
            return $incoming;
        }

        if (AppConfig::isDebug() && $incoming !== '') {
            return $incoming;
        }

        return $origins[0];
    }
}

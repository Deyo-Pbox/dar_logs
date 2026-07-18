<?php

declare(strict_types=1);

namespace DarLogs\Middleware;

use DarLogs\Core\Middleware;
use DarLogs\Core\Request;
use DarLogs\Core\Response;

class CorsMiddleware implements Middleware
{
    public function handle(Request $request, callable $next): Response
    {
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type, Authorization');
        header('Access-Control-Max-Age: 86400');

        if ($request->getMethod() === 'OPTIONS') {
            return Response::ok();
        }

        return $next($request);
    }
}

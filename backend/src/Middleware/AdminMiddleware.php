<?php

declare(strict_types=1);

namespace DarLogs\Middleware;

use DarLogs\Core\Middleware;
use DarLogs\Core\Request;
use DarLogs\Core\Response;
use DarLogs\Exceptions\AuthorizationException;

class AdminMiddleware implements Middleware
{
    public function handle(Request $request, callable $next): Response
    {
        $user = $request->getUser();

        if ($user === null || ($user->role ?? '') !== 'admin') {
            throw new AuthorizationException('Admin access required');
        }

        return $next($request);
    }
}

<?php

declare(strict_types=1);

namespace DarLogs\Middleware;

use DarLogs\Core\Middleware;
use DarLogs\Core\Request;
use DarLogs\Core\Response;
use DarLogs\Exceptions\AuthenticationException;
use DarLogs\Security\JwtHandler;
use Exception;

class AuthMiddleware implements Middleware
{
    public function handle(Request $request, callable $next): Response
    {
        $token = $request->getBearerToken();

        if ($token === null) {
            throw new AuthenticationException('Authentication required');
        }

        try {
            $decoded = JwtHandler::verify($token);
        } catch (Exception) {
            throw new AuthenticationException('Invalid or expired token');
        }

        $request->setUser((object) [
            'id'       => $decoded->sub,
            'username' => $decoded->username,
            'role'     => $decoded->role,
        ]);

        return $next($request);
    }
}

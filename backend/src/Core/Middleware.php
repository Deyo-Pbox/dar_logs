<?php

declare(strict_types=1);

namespace DarLogs\Core;

interface Middleware
{
    public function handle(Request $request, callable $next): Response;
}

<?php

declare(strict_types=1);

namespace DarLogs\Exceptions;

class AuthorizationException extends AppException
{
    public function __construct(string $message = 'Forbidden')
    {
        parent::__construct($message);
    }

    public function getHttpCode(): int
    {
        return 403;
    }
}

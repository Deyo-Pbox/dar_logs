<?php

declare(strict_types=1);

namespace DarLogs\Exceptions;

class AuthenticationException extends AppException
{
    public function __construct(string $message = 'Authentication required')
    {
        parent::__construct($message);
    }

    public function getHttpCode(): int
    {
        return 401;
    }
}

<?php

declare(strict_types=1);

namespace DarLogs\Exceptions;

class ConflictException extends AppException
{
    public function __construct(string $message = 'Resource conflict')
    {
        parent::__construct($message);
    }

    public function getHttpCode(): int
    {
        return 409;
    }
}

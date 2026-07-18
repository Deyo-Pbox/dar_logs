<?php

declare(strict_types=1);

namespace DarLogs\Exceptions;

class NotFoundException extends AppException
{
    public function __construct(string $message = 'Resource not found')
    {
        parent::__construct($message);
    }

    public function getHttpCode(): int
    {
        return 404;
    }
}

<?php

declare(strict_types=1);

namespace DarLogs\Exceptions;

class ValidationException extends AppException
{
    private array $errors;

    public function __construct(string $message = 'Validation failed', array $errors = [])
    {
        parent::__construct($message);
        $this->errors = $errors;
    }

    public function getHttpCode(): int
    {
        return 422;
    }

    public function getErrors(): array
    {
        return $this->errors;
    }
}

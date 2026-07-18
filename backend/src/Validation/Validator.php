<?php

declare(strict_types=1);

namespace DarLogs\Validation;

interface Validator
{
    /** @throws \DarLogs\Exceptions\ValidationException */
    public function validate(array $data): array;
}

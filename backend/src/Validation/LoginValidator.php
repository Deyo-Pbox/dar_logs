<?php

declare(strict_types=1);

namespace DarLogs\Validation;

use DarLogs\Exceptions\ValidationException;

class LoginValidator implements Validator
{
    public function validate(array $data): array
    {
        $errors = [];

        $username = trim($data['username'] ?? '');
        if ($username === '') {
            $errors['username'] = ['Username is required.'];
        }

        $password = $data['password'] ?? '';
        if ($password === '') {
            $errors['password'] = ['Password is required.'];
        }

        if (!empty($errors)) {
            throw new ValidationException('Validation failed', $errors);
        }

        return ['username' => $username, 'password' => $password];
    }
}

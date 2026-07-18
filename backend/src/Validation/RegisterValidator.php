<?php

declare(strict_types=1);

namespace DarLogs\Validation;

use DarLogs\Exceptions\ValidationException;

class RegisterValidator implements Validator
{
    public function validate(array $data): array
    {
        $errors = [];

        $username = trim($data['username'] ?? '');
        if ($username === '') {
            $errors['username'] = ['Username is required.'];
        } elseif (mb_strlen($username) < 3) {
            $errors['username'] = ['Username must be at least 3 characters.'];
        }

        $password = $data['password'] ?? '';
        if ($password === '') {
            $errors['password'] = ['Password is required.'];
        } elseif (strlen($password) < 6) {
            $errors['password'] = ['Password must be at least 6 characters.'];
        }

        if (!empty($errors)) {
            throw new ValidationException('Validation failed', $errors);
        }

        return ['username' => $username, 'password' => $password];
    }
}

<?php

declare(strict_types=1);

namespace DarLogs\Validation;

use DarLogs\Exceptions\ValidationException;

class UserValidator implements Validator
{
    private bool $isUpdate;

    public function __construct(bool $isUpdate = false)
    {
        $this->isUpdate = $isUpdate;
    }

    public function validate(array $data): array
    {
        $errors = [];
        $result = [];

        if (isset($data['username'])) {
            $username = trim($data['username']);
            if ($username === '') {
                $errors['username'] = ['Username is required.'];
            } elseif (mb_strlen($username) < 3) {
                $errors['username'] = ['Username must be at least 3 characters.'];
            }
            $result['username'] = $username;
        } elseif (!$this->isUpdate) {
            $errors['username'] = ['Username is required.'];
        }

        if (isset($data['password'])) {
            $password = $data['password'];
            if ($this->isUpdate && $password === '') {
                // skip password on update if empty (means don't change)
            } elseif (strlen($password) < 6) {
                $errors['password'] = ['Password must be at least 6 characters.'];
            } else {
                $result['password'] = $password;
            }
        } elseif (!$this->isUpdate) {
            $errors['password'] = ['Password is required.'];
        }

        if (isset($data['role'])) {
            if (!in_array($data['role'], ['admin', 'user'], true)) {
                $errors['role'] = ['Role must be admin or user.'];
            }
            $result['role'] = $data['role'];
        }

        if (!empty($errors)) {
            throw new ValidationException('Validation failed', $errors);
        }

        return $result;
    }
}

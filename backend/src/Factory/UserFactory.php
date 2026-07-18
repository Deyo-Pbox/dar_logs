<?php

declare(strict_types=1);

namespace DarLogs\Factory;

use DarLogs\Models\User;

class UserFactory
{
    public static function fromRow(array $row): User
    {
        $user = new User();
        $user->id = (int) $row['id'];
        $user->username = $row['username'];
        $user->role = $row['role'];
        $user->approved = (bool) ($row['approved'] ?? false);
        $user->lastActivity = $row['last_activity'] ?? null;
        $user->createdAt = $row['created_at'] ?? null;
        return $user;
    }
}

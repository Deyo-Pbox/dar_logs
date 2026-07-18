<?php

declare(strict_types=1);

namespace DarLogs\Models;

use JsonSerializable;

class User implements JsonSerializable
{
    public int $id;
    public string $username;
    public string $role;
    public bool $approved;
    public ?string $lastActivity;
    public ?string $createdAt;

    public function jsonSerialize(): array
    {
        return [
            'id'            => $this->id,
            'username'      => $this->username,
            'role'          => $this->role,
            'approved'      => $this->approved,
            'last_activity' => $this->lastActivity,
            'created_at'    => $this->createdAt,
        ];
    }
}

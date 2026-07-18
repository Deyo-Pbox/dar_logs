<?php

declare(strict_types=1);

namespace DarLogs\Models;

use JsonSerializable;

class AuditLog implements JsonSerializable
{
    public int $id;
    public ?int $userId;
    public string $username;
    public string $action;
    public string $tableName;
    public ?int $recordId;
    public ?string $details;
    public ?string $createdAt;

    public function jsonSerialize(): array
    {
        return [
            'id'         => $this->id,
            'user_id'    => $this->userId,
            'username'   => $this->username,
            'action'     => $this->action,
            'table_name' => $this->tableName,
            'record_id'  => $this->recordId,
            'details'    => $this->details,
            'created_at' => $this->createdAt,
        ];
    }
}

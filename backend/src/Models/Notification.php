<?php

declare(strict_types=1);

namespace DarLogs\Models;

use JsonSerializable;

class Notification implements JsonSerializable
{
    public int $id;
    public int $userId;
    public string $type;
    public ?int $recordId;
    public ?int $senderId;
    public string $message;
    public bool $isRead;
    public ?string $createdAt;

    public function jsonSerialize(): array
    {
        return [
            'id'         => $this->id,
            'user_id'    => $this->userId,
            'type'       => $this->type,
            'record_id'  => $this->recordId,
            'sender_id'  => $this->senderId,
            'message'    => $this->message,
            'is_read'    => $this->isRead,
            'created_at' => $this->createdAt,
        ];
    }
}

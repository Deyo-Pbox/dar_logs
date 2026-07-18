<?php

declare(strict_types=1);

namespace DarLogs\Repositories;

use DarLogs\Models\Notification;

interface NotificationRepository
{
    /** @return Notification[] */
    public function findByUser(int $userId, int $limit = 50): array;
    public function countUnread(int $userId): int;
    public function markRead(int $id, int $userId): bool;
    public function create(int $userId, string $type, ?int $recordId, ?int $senderId, string $message): void;
    public function trimForUser(int $userId, int $keep = 50): void;
    public function ensureSchema(): void;
}

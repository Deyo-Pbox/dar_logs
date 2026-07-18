<?php

declare(strict_types=1);

namespace DarLogs\Repositories;

use DarLogs\Models\AuditLog;

interface AuditRepository
{
    /** @return AuditLog[] */
    public function findAll(int $limit = 100): array;
    public function log(int $userId, string $username, string $action, string $tableName, ?int $recordId = null, ?string $details = null): void;
    public function prune(int $capacity): void;
    public function clear(): void;
    public function ensureSchema(): void;
}

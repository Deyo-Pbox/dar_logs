<?php

declare(strict_types=1);

namespace DarLogs\Repositories;

use DarLogs\Models\ActivityLog;

interface ActivityRepository
{
    /** @return ActivityLog[] */
    public function findAll(array $filters = [], int $page = 1, int $perPage = 20): array;
    public function countAll(array $filters = []): int;
    public function findById(int $id): ?ActivityLog;
    /** @return ActivityLog[] */
    public function findByUser(int $userId): array;
    /** @return ActivityLog[] */
    public function findArchived(int $page = 1, int $perPage = 20): array;
    public function countArchived(): int;
    public function countPending(?int $userId = null): int;
    public function countCompleted(): int;
    /** @return ActivityLog[] */
    public function findPending(int $page = 1, int $perPage = 20): array;
    /** @return ActivityLog[] */
    public function findCompleted(int $page = 1, int $perPage = 20): array;
    /** @return ActivityLog[] */
    public function findRecentEdits(int $limit = 5): array;
    public function create(array $fields): int;
    public function update(int $id, array $fields): bool;
    public function archive(int $id, int $archivedBy): bool;
    public function restore(int $id): bool;
    public function delete(int $id): bool;
    public function routeToUser(int $id, int $routeToUserId, int $routedFromUserId): bool;
    public function getDashboardStats(): array;
}

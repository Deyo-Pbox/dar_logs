<?php

declare(strict_types=1);

namespace DarLogs\Repositories;

use DarLogs\Models\User;

interface UserRepository
{
    public function findById(int $id): ?User;
    public function findByUsername(string $username): ?User;
    /** @return User[] */
    public function findAll(): array;
    /** @return User[] */
    public function findAllApproved(): array;
    public function create(string $username, string $passwordHash, string $role = 'user', bool $approved = false): int;
    public function update(int $id, array $fields): bool;
    public function delete(int $id): bool;
    public function updateLastActivity(int $id): void;
    public function findPasswordHash(int $id): ?string;
    public function ensureSchema(): void;
}

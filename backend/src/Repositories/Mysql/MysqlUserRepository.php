<?php

declare(strict_types=1);

namespace DarLogs\Repositories\Mysql;

use DarLogs\Core\Database;
use DarLogs\Models\User;
use DarLogs\Repositories\UserRepository;
use PDO;

class MysqlUserRepository implements UserRepository
{
    private PDO $pdo;

    public function __construct()
    {
        $this->pdo = Database::getInstance();
    }

    public function findById(int $id): ?User
    {
        $stmt = $this->pdo->prepare(
            'SELECT id, username, role, approved, last_activity, created_at FROM users WHERE id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ? $this->hydrate($row) : null;
    }

    public function findByUsername(string $username): ?User
    {
        $stmt = $this->pdo->prepare(
            'SELECT id, username, role, approved, last_activity, created_at FROM users WHERE username = ?'
        );
        $stmt->execute([$username]);
        $row = $stmt->fetch();
        return $row ? $this->hydrate($row) : null;
    }

    public function findPasswordHash(int $id): ?string
    {
        $stmt = $this->pdo->prepare('SELECT password FROM users WHERE id = ?');
        $stmt->execute([$id]);
        return $stmt->fetchColumn() ?: null;
    }

    public function findAll(): array
    {
        $stmt = $this->pdo->query(
            'SELECT id, username, role, approved, last_activity, created_at FROM users ORDER BY id'
        );
        return array_map([$this, 'hydrate'], $stmt->fetchAll());
    }

    public function findAllApproved(): array
    {
        $stmt = $this->pdo->query(
            'SELECT id, username, role, approved, last_activity, created_at FROM users WHERE approved = 1 ORDER BY username'
        );
        return array_map([$this, 'hydrate'], $stmt->fetchAll());
    }

    public function create(string $username, string $passwordHash, string $role = 'user', bool $approved = false): int
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO users (username, password, role, approved) VALUES (?, ?, ?, ?)'
        );
        $stmt->execute([$username, $passwordHash, $role, $approved ? 1 : 0]);
        return (int) $this->pdo->lastInsertId();
    }

    public function update(int $id, array $fields): bool
    {
        if (empty($fields)) {
            return false;
        }
        $sets = [];
        $params = [];
        foreach ($fields as $col => $val) {
            $sets[] = "`{$col}` = ?";
            $params[] = $val;
        }
        $params[] = $id;
        $stmt = $this->pdo->prepare('UPDATE users SET ' . implode(', ', $sets) . ' WHERE id = ?');
        $stmt->execute($params);
        return $stmt->rowCount() > 0;
    }

    public function delete(int $id): bool
    {
        $stmt = $this->pdo->prepare('DELETE FROM users WHERE id = ?');
        $stmt->execute([$id]);
        return $stmt->rowCount() > 0;
    }

    public function updateLastActivity(int $id): void
    {
        $stmt = $this->pdo->prepare('UPDATE users SET last_activity = NOW() WHERE id = ?');
        $stmt->execute([$id]);
    }

    public function ensureSchema(): void
    {
        static $checked = false;
        if ($checked) return;
        $checked = true;

        $existing = [];
        $rows = $this->pdo->query('SHOW INDEX FROM users')->fetchAll();
        foreach ($rows as $row) {
            $existing[$row['Key_name']] = true;
        }

        $indexes = [
            'idx_users_approved'       => 'ALTER TABLE users ADD INDEX idx_users_approved (approved)',
            'idx_users_last_activity'  => 'ALTER TABLE users ADD INDEX idx_users_last_activity (last_activity)',
            'idx_users_approved_active'=> 'ALTER TABLE users ADD INDEX idx_users_approved_active (approved, last_activity)',
        ];

        foreach ($indexes as $name => $sql) {
            if (!isset($existing[$name])) {
                $this->pdo->exec($sql);
            }
        }
    }

    private function hydrate(array $row): User
    {
        $user = new User();
        $user->id = (int) $row['id'];
        $user->username = $row['username'];
        $user->role = $row['role'];
        $user->approved = (bool) $row['approved'];
        $user->lastActivity = $row['last_activity'] ?? null;
        $user->createdAt = $row['created_at'] ?? null;
        return $user;
    }
}

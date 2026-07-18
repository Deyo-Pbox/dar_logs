<?php

declare(strict_types=1);

namespace DarLogs\Repositories\Mysql;

use DarLogs\Core\Database;
use DarLogs\Models\AuditLog;
use DarLogs\Repositories\AuditRepository;
use PDO;

class MysqlAuditRepository implements AuditRepository
{
    private PDO $pdo;

    public function __construct()
    {
        $this->pdo = Database::getInstance();
    }

    public function findAll(int $limit = 100): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT id, user_id, username, action, table_name, record_id, details, created_at
            FROM audit_log ORDER BY created_at DESC LIMIT ?'
        );
        $stmt->execute([$limit]);
        return array_map([$this, 'hydrate'], $stmt->fetchAll());
    }

    public function log(int $userId, string $username, string $action, string $tableName, ?int $recordId = null, ?string $details = null): void
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO audit_log (user_id, username, action, table_name, record_id, details) VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([$userId, $username, $action, $tableName, $recordId, $details]);
    }

    public function prune(int $capacity): void
    {
        $cap = max(1, min(1000, $capacity));
        $n = (int) $this->pdo->query('SELECT COUNT(*) FROM audit_log')->fetchColumn();
        if ($n <= $cap) return;

        $this->pdo->exec(
            'DELETE FROM audit_log WHERE id NOT IN ('
            . 'SELECT id FROM (SELECT id FROM audit_log ORDER BY created_at DESC, id DESC LIMIT ' . $cap
            . ') AS keep_rows)'
        );
    }

    public function clear(): void
    {
        $this->pdo->exec('TRUNCATE TABLE audit_log');
    }

    public function ensureSchema(): void
    {
        static $checked = false;
        if ($checked) return;
        $checked = true;

        $this->pdo->exec("CREATE TABLE IF NOT EXISTS audit_log (
            id INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            user_id INT(11) UNSIGNED DEFAULT NULL,
            username VARCHAR(64) NOT NULL DEFAULT 'Unknown',
            action ENUM('add','edit','delete','archive','restore','login','logout') NOT NULL DEFAULT 'add',
            table_name VARCHAR(64) NOT NULL DEFAULT '',
            record_id INT(11) UNSIGNED DEFAULT NULL,
            details TEXT DEFAULT NULL,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id),
            KEY idx_audit_recent_actions (created_at, action),
            KEY idx_audit_record_id (record_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private function hydrate(array $row): AuditLog
    {
        $log = new AuditLog();
        $log->id = (int) ($row['id'] ?? 0);
        $log->userId = isset($row['user_id']) ? (int) $row['user_id'] : null;
        $log->username = (string) ($row['username'] ?? 'Unknown');
        $log->action = (string) ($row['action'] ?? 'unknown');
        $log->tableName = (string) ($row['table_name'] ?? '');
        $log->recordId = isset($row['record_id']) ? (int) $row['record_id'] : null;
        $log->details = $row['details'] ?? null;
        $log->createdAt = $row['created_at'] ?? null;
        return $log;
    }
}

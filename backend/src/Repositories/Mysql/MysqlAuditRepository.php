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

        $stmt = $this->pdo->query("SHOW COLUMNS FROM audit_log LIKE 'action'");
        $col = $stmt ? $stmt->fetch(PDO::FETCH_ASSOC) : false;
        if ($col) {
            $type = strtolower((string) ($col['Type'] ?? ''));
            if (strpos($type, 'archive') === false || strpos($type, 'restore') === false) {
                $this->pdo->exec("ALTER TABLE audit_log MODIFY action ENUM('add','edit','delete','archive','restore','login','logout') NOT NULL");
            }
        }

        $this->ensureIndexes();
    }

    private function ensureIndexes(): void
    {
        $existing = [];
        $rows = $this->pdo->query('SHOW INDEX FROM audit_log')->fetchAll();
        foreach ($rows as $row) {
            $existing[$row['Key_name']] = true;
        }

        if (!isset($existing['idx_audit_recent_actions'])) {
            $this->pdo->exec('ALTER TABLE audit_log ADD INDEX idx_audit_recent_actions (created_at, action)');
        }

        if (!isset($existing['idx_audit_record_id'])) {
            $this->pdo->exec('ALTER TABLE audit_log ADD INDEX idx_audit_record_id (record_id)');
        }
    }

    private function hydrate(array $row): AuditLog
    {
        $log = new AuditLog();
        $log->id = (int) $row['id'];
        $log->userId = isset($row['user_id']) ? (int) $row['user_id'] : null;
        $log->username = $row['username'];
        $log->action = $row['action'];
        $log->tableName = $row['table_name'];
        $log->recordId = isset($row['record_id']) ? (int) $row['record_id'] : null;
        $log->details = $row['details'] ?? null;
        $log->createdAt = $row['created_at'] ?? null;
        return $log;
    }
}

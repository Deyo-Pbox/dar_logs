<?php
/**
 * DAR Activity Logs - Audit Log Helper
 */

/** Maximum rows stored in audit_log; oldest rows are deleted when this is exceeded. */
define('AUDIT_LOG_CAPACITY', 10);

function ensureAuditLogSchema(PDO $pdo): void {
    static $checked = false;
    if ($checked) {
        return;
    }
    $checked = true;

    $stmt = $pdo->query("SHOW COLUMNS FROM audit_log LIKE 'action'");
    $col = $stmt ? $stmt->fetch(PDO::FETCH_ASSOC) : false;
    if ($col) {
        $type = strtolower((string) ($col['Type'] ?? ''));
        if (strpos($type, 'archive') === false || strpos($type, 'restore') === false) {
            $pdo->exec("ALTER TABLE audit_log MODIFY action ENUM('add','edit','delete','archive','restore','login','logout') NOT NULL");
        }
    }

    ensureAuditLogIndexes($pdo);
}

function ensureAuditLogIndexes(PDO $pdo): void
{
    static $indexChecked = false;
    if ($indexChecked) return;
    $indexChecked = true;

    $existing = [];
    $rows = $pdo->query('SHOW INDEX FROM audit_log')->fetchAll();
    foreach ($rows as $row) {
        $existing[$row['Key_name']] = true;
    }

    if (!isset($existing['idx_audit_recent_actions'])) {
        $pdo->exec('ALTER TABLE audit_log ADD INDEX idx_audit_recent_actions (created_at, action)');
    }
}

/**
 * Keep only the newest AUDIT_LOG_CAPACITY rows (by created_at, then id). Call after new audit rows are inserted.
 */
function pruneAuditLogTable(PDO $pdo, ?int $maxRows = null): void {
    $cap = $maxRows ?? (int) AUDIT_LOG_CAPACITY;
    $cap = max(1, min(100, $cap));
    ensureAuditLogSchema($pdo);
    $n = (int) $pdo->query('SELECT COUNT(*) FROM audit_log')->fetchColumn();
    if ($n <= $cap) {
        return;
    }
    $sql = 'DELETE FROM audit_log WHERE id NOT IN (
        SELECT id FROM (
            SELECT id FROM audit_log ORDER BY created_at DESC, id DESC LIMIT ' . $cap . '
        ) AS keep_rows
    )';
    $pdo->exec($sql);
}

function logAudit(string $action, ?int $recordId = null, ?string $details = null): void {
    $user = currentUser();
    if (!$user) return;
    $pdo = getDB();
    ensureAuditLogSchema($pdo);
    $stmt = $pdo->prepare('INSERT INTO audit_log (user_id, username, action, table_name, record_id, details) VALUES (?, ?, ?, ?, ?, ?)');
    $stmt->execute([
        $user['id'],
        $user['username'],
        $action,
        'activity_logs',
        $recordId,
        $details,
    ]);
    pruneAuditLogTable($pdo);
}

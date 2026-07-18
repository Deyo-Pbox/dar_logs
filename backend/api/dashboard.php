<?php
/**
 * DAR Activity Logs - Dashboard stats (single query, cached 30s)
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/activity_logs.php';
require_once __DIR__ . '/../includes/cache.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
ensureActivityLogsSchema($pdo);
updateLastActivity();

$user = currentUser();

$stats = darCacheGet('dashboard_stats');
if ($stats === null) {
    $row = $pdo->query(
        'SELECT
            (SELECT COUNT(*) FROM activity_logs WHERE archived_at IS NULL) AS total_records,
            (SELECT COUNT(*) FROM activity_logs WHERE archived_at IS NOT NULL) AS archived_records,
            (SELECT COUNT(*) FROM users) AS total_users,
            (SELECT COUNT(*) FROM audit_log WHERE action IN (\'add\',\'edit\',\'delete\',\'archive\',\'restore\') AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)) AS recent_edits,
            (SELECT COUNT(*) FROM users WHERE last_activity >= DATE_SUB(NOW(), INTERVAL 15 MINUTE) AND approved = 1) AS active_users'
    )->fetch();

    $stats = [
        'total_records'    => (int) $row['total_records'],
        'archived_records' => (int) $row['archived_records'],
        'total_users'      => (int) $row['total_users'],
        'recent_edits'     => (int) $row['recent_edits'],
        'active_users'     => (int) $row['active_users'],
    ];
    darCacheSet('dashboard_stats', $stats, 30);
}

echo json_encode([
    'success' => true,
    'stats'   => $stats,
    'user'    => [
        'id'       => (int) $user['id'],
        'username' => $user['username'],
        'role'     => $user['role'],
    ],
]);

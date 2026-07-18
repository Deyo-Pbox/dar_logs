<?php
/**
 * DAR Activity Logs - Audit Log API (for dashboard)
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/audit.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
updateLastActivity();
$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    pruneAuditLogTable($pdo);
    // Default matches AUDIT_LOG_CAPACITY (only that many rows are stored).
    $defaultLimit = defined('AUDIT_LOG_CAPACITY') ? (int) AUDIT_LOG_CAPACITY : 10;
    $limit = min(200, max(1, (int) ($_GET['limit'] ?? $defaultLimit)));
    $stmt = $pdo->prepare('SELECT id, username, action, record_id, details, created_at FROM audit_log ORDER BY created_at DESC LIMIT ?');
    $stmt->execute([$limit]);
    $rows = $stmt->fetchAll();
    echo json_encode(['success' => true, 'data' => $rows]);
    exit;
}

if ($method === 'DELETE') {
    requireAdmin();
    $id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
    if ($id <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid log ID']);
        exit;
    }
    $stmt = $pdo->prepare('DELETE FROM audit_log WHERE id = ?');
    $stmt->execute([$id]);
    if ($stmt->rowCount() < 1) {
        echo json_encode(['success' => false, 'message' => 'Log not found']);
        exit;
    }
    echo json_encode(['success' => true, 'message' => 'Log deleted']);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'message' => 'Method not allowed']);

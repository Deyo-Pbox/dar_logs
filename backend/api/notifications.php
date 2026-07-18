<?php
/**
 * DAR Activity Logs - Notifications API
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/notifications.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
ensureNotificationsSchema($pdo);

$method = $_SERVER['REQUEST_METHOD'];
$user = currentUser();
$userId = (int) $user['id'];
trimNotificationsForUser($pdo, $userId);

if ($method === 'GET') {
    updateLastActivity();
    $onlyUnread = isset($_GET['unread']) && $_GET['unread'] === '1';
    $countOnly = isset($_GET['count']) && $_GET['count'] === '1';
    $limit = isset($_GET['limit']) ? max(1, min(DAR_NOTIFICATION_HISTORY_LIMIT, (int) $_GET['limit'])) : DAR_NOTIFICATION_HISTORY_LIMIT;
    $scopeAll = isset($_GET['scope']) && $_GET['scope'] === 'all';

    if ($countOnly) {
        $sql = "SELECT COUNT(*) FROM notifications";
        if (!$scopeAll) {
            $sql .= " WHERE user_id = ?";
        }
        if ($onlyUnread) {
            $sql .= $scopeAll ? " WHERE is_read = 0" : " AND is_read = 0";
        }
        $stmt = $pdo->prepare($sql);
        if ($scopeAll) {
            $stmt->execute();
        } else {
            $stmt->execute([$userId]);
        }
        $count = (int) $stmt->fetchColumn();
        echo json_encode(['success' => true, 'count' => $count]);
        exit;
    }

    $sql = "
        SELECT n.id, n.type, n.record_id, n.sender_id, n.message, n.is_read, n.created_at,
               u.username AS sender_name
        FROM notifications n
        LEFT JOIN users u ON n.sender_id = u.id
    ";
    if (!$scopeAll) {
        $sql .= " WHERE n.user_id = ?";
    }
    if ($onlyUnread) {
        $sql .= $scopeAll ? " WHERE n.is_read = 0" : " AND n.is_read = 0";
    }
    $sql .= " ORDER BY n.created_at DESC, n.id DESC LIMIT " . (int) $limit;

    $stmt = $pdo->prepare($sql);
    if ($scopeAll) {
        $stmt->execute();
    } else {
        $stmt->execute([$userId]);
    }
    echo json_encode(['success' => true, 'data' => $stmt->fetchAll()]);
    exit;
}

if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $action = $input['action'] ?? '';

    if ($action === 'mark_read') {
        $ids = $input['ids'] ?? ($input['id'] ?? null);
        if (!is_array($ids)) {
            $ids = [$ids];
        }
        $clean = [];
        foreach ($ids as $raw) {
            $id = (int) $raw;
            if ($id > 0 && !in_array($id, $clean, true)) {
                $clean[] = $id;
            }
        }
        if (!$clean) {
            echo json_encode(['success' => false, 'message' => 'No notification IDs provided']);
            exit;
        }

        $placeholders = implode(',', array_fill(0, count($clean), '?'));
        $params = array_merge([$userId], $clean);
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND id IN ($placeholders)");
        $stmt->execute($params);
        echo json_encode(['success' => true, 'updated' => $stmt->rowCount()]);
        exit;
    }

    echo json_encode(['success' => false, 'message' => 'Invalid action']);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'message' => 'Method not allowed']);

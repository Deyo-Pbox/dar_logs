<?php
/**
 * DAR Activity Logs - Pending Records API
 *
 * Returns records where work_status = 'not_finished' and archived_at IS NULL.
 * Admins see all users' pending records; regular users see only their own.
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/activity_logs.php';
require_once __DIR__ . '/../includes/audit.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
ensureActivityLogsSchema($pdo);

$method = $_SERVER['REQUEST_METHOD'];
$user = currentUser();
$userId = (int) $user['id'];
$isAdmin = isAdmin();

if ($method === 'GET') {
    updateLastActivity();

    $sql = "
        SELECT
            al.id,
            al.municipality,
            al.lo_claimant,
            al.title_no,
            al.odts_no,
            al.lot_no,
            al.survey_no,
            al.area_has,
            al.location,
            al.transmitted_documents,
            al.route_to,
            al.received_by_control_no,
            al.remarks_action_taken,
            al.work_status,
            al.created_at,
            al.updated_at,
            u1.username AS created_by_name,
            u2.username AS updated_by_name
        FROM activity_logs al
        LEFT JOIN users u1 ON al.created_by = u1.id
        LEFT JOIN users u2 ON al.updated_by = u2.id
        WHERE al.archived_at IS NULL
          AND al.work_status = 'not_finished'
    ";

    if ($isAdmin) {
        $sql .= " ORDER BY al.updated_at DESC, al.created_at DESC";
        $stmt = $pdo->query($sql);
    } else {
        $sql .= " AND (al.created_by = ? OR al.route_to_user_id = ?)";
        $sql .= " ORDER BY al.updated_at DESC, al.created_at DESC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$userId, $userId]);
    }

    $rows = $stmt ? $stmt->fetchAll() : [];
    echo json_encode(['success' => true, 'data' => $rows]);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'message' => 'Method not allowed']);

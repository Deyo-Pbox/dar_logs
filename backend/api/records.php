<?php
/**
 * DAR Activity Logs - Records API (CRUD)
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/activity_logs.php';
require_once __DIR__ . '/../includes/audit.php';
require_once __DIR__ . '/../includes/notifications.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
$method = $_SERVER['REQUEST_METHOD'];

ensureActivityLogsSchema($pdo);
ensureNotificationsSchema($pdo);

function fetchRecordState(PDO $pdo, int $id): ?array {
    $stmt = $pdo->prepare('SELECT id, archived_at FROM activity_logs WHERE id = ?');
    $stmt->execute([$id]);
    $row = $stmt->fetch();
    return $row ?: null;
}

// GET - List all active records
if ($method === 'GET') {
    updateLastActivity();
    $scopeAll = isset($_GET['scope']) && $_GET['scope'] === 'all';
    $sql = "
        SELECT al.id, al.municipality, al.lo_claimant, al.title_no, al.odts_no, al.lot_no, al.survey_no,
               al.area_has, al.location, al.transmitted_documents, al.route_to,
               al.route_to_user_id, al.routed_from_user_id, al.routed_at,
               al.received_by_control_no, al.remarks_action_taken, al.work_status,
               al.created_at, al.updated_at,
               u1.username AS created_by_name, u2.username AS updated_by_name,
               u3.username AS routed_from_name
        FROM activity_logs al
        LEFT JOIN users u1 ON al.created_by = u1.id
        LEFT JOIN users u2 ON al.updated_by = u2.id
        LEFT JOIN users u3 ON al.routed_from_user_id = u3.id
        WHERE al.archived_at IS NULL
    ";

    if (!$scopeAll) {
        $user = currentUser();
        $userId = (int) $user['id'];
        $sql .= ' AND (al.created_by = ? OR al.route_to_user_id = ?)';
    }

    $sql .= ' ORDER BY al.updated_at DESC';

    if ($scopeAll) {
        $stmt = $pdo->query($sql);
    } else {
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$userId, $userId]);
    }

    $rows = $stmt->fetchAll();
    echo json_encode(['success' => true, 'data' => $rows]);
    exit;
}

// POST - Create record
if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $user = currentUser();
    $municipality = normalizeMunicipalityInput($input['municipality'] ?? null);
    if ($municipality === null) {
        echo json_encode(['success' => false, 'message' => 'This is not a valid municipality. Please select from the list.']);
        exit;
    }
    $workStatus = normalizeWorkStatus($input['work_status'] ?? null);
    $routeToUserId = isset($input['route_to_user_id']) ? (int) $input['route_to_user_id'] : 0;
    $routeToUserName = '';
    if ($routeToUserId > 0) {
        $u = $pdo->prepare('SELECT id, username FROM users WHERE id = ? AND approved = 1');
        $u->execute([$routeToUserId]);
        $userRow = $u->fetch();
        if ($userRow) {
            $routeToUserName = (string) $userRow['username'];
        } else {
            $routeToUserId = 0;
        }
    }

    $stmt = $pdo->prepare("
        INSERT INTO activity_logs (
            municipality, lo_claimant, title_no, odts_no, lot_no, survey_no, area_has, location,
            transmitted_documents, route_to, route_to_user_id, routed_from_user_id, routed_at,
            received_by_control_no, remarks_action_taken, work_status, created_by, updated_by
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?, ?)
    ");
    $stmt->execute([
        $municipality,
        trim($input['lo_claimant'] ?? ''),
        trim($input['title_no'] ?? ''),
        trim($input['odts_no'] ?? ''),
        trim($input['lot_no'] ?? ''),
        trim($input['survey_no'] ?? ''),
        isset($input['area_has']) ? (float) $input['area_has'] : null,
        trim($input['location'] ?? ''),
        trim($input['transmitted_documents'] ?? ''),
        $routeToUserName !== '' ? $routeToUserName : trim($input['route_to'] ?? ''),
        $routeToUserId > 0 ? $routeToUserId : null,
        $routeToUserId > 0 ? (int) $user['id'] : null,
        trim($input['received_by_control_no'] ?? ''),
        trim($input['remarks_action_taken'] ?? ''),
        $workStatus,
        $user['id'],
        $user['id'],
    ]);
    $id = (int) $pdo->lastInsertId();
    logAudit('add', $id, 'Record created');
    if ($routeToUserId > 0) {
        createRouteNotification($pdo, $routeToUserId, (int) $user['id'], $id, (string) $user['username']);
    }
    echo json_encode(['success' => true, 'id' => $id]);
    exit;
}

// PUT - Update record
if ($method === 'PUT') {
    if (!isAdmin()) {
        http_response_code(403);
        echo json_encode(['success' => false, 'message' => 'Admin only']);
        exit;
    }
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $id = isset($input['id']) ? (int) $input['id'] : 0;
    $municipality = normalizeMunicipalityInput($input['municipality'] ?? null);
    if ($municipality === null) {
        echo json_encode(['success' => false, 'message' => 'This is not a valid municipality. Please select from the list.']);
        exit;
    }
    $workStatus = normalizeWorkStatus($input['work_status'] ?? null);
    $routeToUserId = isset($input['route_to_user_id']) ? (int) $input['route_to_user_id'] : 0;
    $routeToUserName = '';
    if ($routeToUserId > 0) {
        $u = $pdo->prepare('SELECT id, username FROM users WHERE id = ? AND approved = 1');
        $u->execute([$routeToUserId]);
        $userRow = $u->fetch();
        if ($userRow) {
            $routeToUserName = (string) $userRow['username'];
        } else {
            $routeToUserId = 0;
        }
    }
    if ($id <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid record ID']);
        exit;
    }

    $meta = fetchRecordState($pdo, $id);
    if (!$meta) {
        echo json_encode(['success' => false, 'message' => 'Record not found']);
        exit;
    }
    if (!empty($meta['archived_at'])) {
        echo json_encode(['success' => false, 'message' => 'Archived records must be restored before editing']);
        exit;
    }

    $user = currentUser();
    $prev = $pdo->prepare('SELECT route_to_user_id FROM activity_logs WHERE id = ?');
    $prev->execute([$id]);
    $prevRow = $prev->fetch();
    $prevRouteToUserId = $prevRow ? (int) ($prevRow['route_to_user_id'] ?? 0) : 0;
    $routeChanged = $routeToUserId > 0 && $routeToUserId !== $prevRouteToUserId;

    $stmt = $pdo->prepare("
        UPDATE activity_logs SET
            municipality = ?, lo_claimant = ?, title_no = ?, odts_no = ?, lot_no = ?, survey_no = ?,
            area_has = ?, location = ?, transmitted_documents = ?,
            route_to = ?, route_to_user_id = ?, routed_from_user_id = ?, routed_at = ?,
            received_by_control_no = ?, remarks_action_taken = ?, work_status = ?, updated_by = ?
        WHERE id = ?
    ");
    $stmt->execute([
        $municipality,
        trim($input['lo_claimant'] ?? ''),
        trim($input['title_no'] ?? ''),
        trim($input['odts_no'] ?? ''),
        trim($input['lot_no'] ?? ''),
        trim($input['survey_no'] ?? ''),
        isset($input['area_has']) ? (float) $input['area_has'] : null,
        trim($input['location'] ?? ''),
        trim($input['transmitted_documents'] ?? ''),
        $routeToUserName !== '' ? $routeToUserName : trim($input['route_to'] ?? ''),
        $routeToUserId > 0 ? $routeToUserId : null,
        $routeToUserId > 0 ? (int) $user['id'] : null,
        $routeToUserId > 0 ? date('Y-m-d H:i:s') : null,
        trim($input['received_by_control_no'] ?? ''),
        trim($input['remarks_action_taken'] ?? ''),
        $workStatus,
        $user['id'],
        $id,
    ]);
    if ($stmt->rowCount()) {
        logAudit('edit', $id, 'Record updated');
    }
    if ($routeChanged) {
        createRouteNotification($pdo, $routeToUserId, (int) $user['id'], $id, (string) $user['username']);
    }
    echo json_encode(['success' => true]);
    exit;
}

// DELETE - Delete record (admin only)
if ($method === 'DELETE') {
    if (!isAdmin()) {
        http_response_code(403);
        echo json_encode(['success' => false, 'message' => 'Admin only']);
        exit;
    }
    $id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
    if ($id <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid record ID']);
        exit;
    }

    $meta = fetchRecordState($pdo, $id);
    if (!$meta) {
        echo json_encode(['success' => false, 'message' => 'Record not found']);
        exit;
    }
    if (!empty($meta['archived_at'])) {
        echo json_encode(['success' => false, 'message' => 'Archived records must be restored before deleting']);
        exit;
    }

    $stmt = $pdo->prepare('DELETE FROM activity_logs WHERE id = ? AND archived_at IS NULL');
    $stmt->execute([$id]);
    if ($stmt->rowCount()) {
        logAudit('delete', $id, 'Record deleted');
    }
    echo json_encode(['success' => true]);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'message' => 'Method not allowed']);

<?php
/**
 * DAR Activity Logs - User-scoped work logs API
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/activity_logs.php';
require_once __DIR__ . '/../includes/audit.php';
require_once __DIR__ . '/../includes/notifications.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
$method = $_SERVER['REQUEST_METHOD'];
$user = currentUser();
$userId = (int) $user['id'];

ensureActivityLogsSchema($pdo);
ensureNotificationsSchema($pdo);

function normalizeDedupeValue($value): string {
    if ($value === null) {
        return '';
    }
    if (is_bool($value)) {
        return $value ? '1' : '0';
    }
    return preg_replace('/\s+/', ' ', trim((string) $value));
}

function dedupeKey(array $row): string {
    $parts = [
        normalizeDedupeValue($row['municipality'] ?? ''),
        normalizeDedupeValue($row['lo_claimant'] ?? ''),
        normalizeDedupeValue($row['title_no'] ?? ''),
        normalizeDedupeValue($row['odts_no'] ?? ''),
        normalizeDedupeValue($row['lot_no'] ?? ''),
        normalizeDedupeValue($row['survey_no'] ?? ''),
        normalizeDedupeValue($row['area_has'] ?? ''),
        normalizeDedupeValue($row['location'] ?? ''),
        normalizeDedupeValue($row['transmitted_documents'] ?? ''),
        normalizeDedupeValue($row['route_to'] ?? ''),
        normalizeDedupeValue($row['received_by_control_no'] ?? ''),
        normalizeDedupeValue($row['remarks_action_taken'] ?? ''),
        normalizeDedupeValue($row['work_status'] ?? ''),
    ];
    return hash('sha256', implode("\n", $parts));
}

// (Legacy) owner-only record state helper removed; routed records can be edited by the receiver.

if ($method === 'GET') {
    updateLastActivity();
    $stmt = $pdo->prepare("
        SELECT al.id, al.municipality, al.lo_claimant, al.title_no, al.odts_no, al.lot_no, al.survey_no,
               al.area_has, al.location, al.transmitted_documents, al.route_to,
               al.route_to_user_id, al.routed_from_user_id, al.routed_at,
               al.received_by_control_no, al.remarks_action_taken, al.work_status,
               al.created_at, al.updated_at,
               u1.username AS created_by_name, u2.username AS updated_by_name,
               u3.username AS routed_from_name,
               CASE WHEN EXISTS (
                   SELECT 1
                   FROM notifications n
                   WHERE n.user_id = ?
                     AND n.record_id = al.id
                     AND n.type = 'route'
                     AND n.is_read = 0
               ) THEN 1 ELSE 0 END AS has_unread_route_notification
        FROM activity_logs al
        LEFT JOIN users u1 ON al.created_by = u1.id
        LEFT JOIN users u2 ON al.updated_by = u2.id
        LEFT JOIN users u3 ON al.routed_from_user_id = u3.id
        WHERE (al.created_by = ? OR al.route_to_user_id = ?)
          AND al.archived_at IS NULL
        ORDER BY al.updated_at DESC
    ");
    $stmt->execute([$userId, $userId, $userId]);
    $rows = $stmt->fetchAll();
    echo json_encode([
        'success' => true,
        'data' => $rows,
        'user' => [
            'id' => $userId,
            'username' => $user['username'],
            'role' => $user['role']
        ]
    ]);
    exit;
}

if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];

    if (($input['action'] ?? '') === 'dedupe') {
        updateLastActivity();
        // Remove likely double-submits: exact same content within a short time window.
        $windowSeconds = 120;
        $stmt = $pdo->prepare("
            SELECT id, municipality, lo_claimant, title_no, odts_no, lot_no, survey_no, area_has, location,
                   transmitted_documents, route_to, received_by_control_no, remarks_action_taken, work_status,
                   created_at, updated_at
            FROM activity_logs
            WHERE created_by = ?
              AND archived_at IS NULL
            ORDER BY updated_at DESC, id DESC
        ");
        $stmt->execute([$userId]);
        $rows = $stmt->fetchAll();

        $seen = [];
        $deleteIds = [];

        foreach ($rows as $row) {
            $key = dedupeKey($row);
            $createdTs = strtotime((string) $row['created_at']) ?: 0;
            $updatedTs = strtotime((string) $row['updated_at']) ?: 0;
            if (!isset($seen[$key])) {
                $seen[$key] = ['created' => $createdTs, 'updated' => $updatedTs];
                continue;
            }

            $deltaCreated = abs($createdTs - (int) $seen[$key]['created']);
            $deltaUpdated = abs($updatedTs - (int) $seen[$key]['updated']);
            if ($deltaCreated <= $windowSeconds || $deltaUpdated <= $windowSeconds) {
                $deleteIds[] = (int) $row['id'];
            }
        }

        if (!$deleteIds) {
            echo json_encode(['success' => true, 'deleted' => 0]);
            exit;
        }

        $pdo->beginTransaction();
        try {
            $placeholders = implode(',', array_fill(0, count($deleteIds), '?'));
            $params = array_merge($deleteIds, [$userId]);
            $del = $pdo->prepare("DELETE FROM activity_logs WHERE id IN ($placeholders) AND created_by = ?");
            $del->execute($params);
            $deleted = $del->rowCount();
            logAudit('delete', null, 'Removed duplicate work log records (count: ' . $deleted . ').');
            $pdo->commit();
        } catch (Throwable $e) {
            $pdo->rollBack();
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to remove duplicates.']);
            exit;
        }

        echo json_encode(['success' => true, 'deleted' => $deleted]);
        exit;
    }

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
        $routeToUserId > 0 ? $userId : null,
        trim($input['received_by_control_no'] ?? ''),
        trim($input['remarks_action_taken'] ?? ''),
        $workStatus,
        $userId,
        $userId,
    ]);
    $id = (int) $pdo->lastInsertId();
    logAudit('add', $id, 'Record created from My Work Logs');
    if ($routeToUserId > 0) {
        createRouteNotification($pdo, $routeToUserId, $userId, $id, (string) $user['username']);
    }
    echo json_encode(['success' => true, 'id' => $id]);
    exit;
}

if ($method === 'PUT') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $id = isset($input['id']) ? (int) $input['id'] : 0;
    $municipality = normalizeMunicipalityInput($input['municipality'] ?? null);
    if ($municipality === null) {
        echo json_encode(['success' => false, 'message' => 'This is not a valid municipality. Please select from the list.']);
        exit;
    }
    $workStatus = normalizeWorkStatus($input['work_status'] ?? null);
    if ($id <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid record ID']);
        exit;
    }

    $check = $pdo->prepare('SELECT id, archived_at, created_by, route_to_user_id FROM activity_logs WHERE id = ?');
    $check->execute([$id]);
    $row = $check->fetch();
    if (!$row) {
        echo json_encode(['success' => false, 'message' => 'Record not found']);
        exit;
    }
    if (!empty($row['archived_at'])) {
        echo json_encode(['success' => false, 'message' => 'Archived records must be restored before editing']);
        exit;
    }
    $canEdit = ((int) $row['created_by'] === $userId) || ((int) ($row['route_to_user_id'] ?? 0) === $userId);
    if (!$canEdit) {
        echo json_encode(['success' => false, 'message' => 'Record not found or not assigned to current user']);
        exit;
    }

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

    $prevRouteToUserId = (int) ($row['route_to_user_id'] ?? 0);
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
        $routeToUserId > 0 ? $userId : null,
        $routeToUserId > 0 ? date('Y-m-d H:i:s') : null,
        trim($input['received_by_control_no'] ?? ''),
        trim($input['remarks_action_taken'] ?? ''),
        $workStatus,
        $userId,
        $id,
    ]);
    if ($routeChanged) {
        createRouteNotification($pdo, $routeToUserId, $userId, $id, (string) $user['username']);
    }
    logAudit('edit', $id, 'Record updated from My Work Logs');
    echo json_encode(['success' => true]);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'message' => 'Method not allowed']);

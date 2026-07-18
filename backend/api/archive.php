<?php
/**
 * DAR Activity Logs - Archive API
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/activity_logs.php';
require_once __DIR__ . '/../includes/audit.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
ensureActivityLogsSchema($pdo);
ensureAuditLogSchema($pdo);

$method = $_SERVER['REQUEST_METHOD'];
$user = currentUser();
$userId = (int) $user['id'];

function normalizeIdList($value): array {
    if (!is_array($value)) {
        $value = [$value];
    }

    $ids = [];
    foreach ($value as $raw) {
        $id = (int) $raw;
        if ($id > 0 && !in_array($id, $ids, true)) {
            $ids[] = $id;
        }
    }
    return $ids;
}

function fetchArchivedIds(PDO $pdo, int $userId): array {
    $stmt = $pdo->prepare('SELECT id FROM activity_logs WHERE archived_at IS NOT NULL AND archived_by = ? ORDER BY archived_at DESC');
    if (!$stmt) {
        return [];
    }
    $stmt->execute([$userId]);
    $ids = array_map('intval', $stmt->fetchAll(PDO::FETCH_COLUMN));
    return array_values(array_filter($ids, static fn($id) => $id > 0));
}

function applyArchiveAction(PDO $pdo, array $ids, string $action, int $userId, bool $isAdmin): int {
    $updated = 0;
    $details = $action === 'archive' ? 'Record archived' : 'Record restored';

    $pdo->beginTransaction();
    try {
        if ($action === 'archive') {
            $stmt = $isAdmin
                ? $pdo->prepare('UPDATE activity_logs SET archived_at = NOW(), archived_by = ?, updated_by = ? WHERE id = ? AND archived_at IS NULL')
                : $pdo->prepare('UPDATE activity_logs SET archived_at = NOW(), archived_by = ?, updated_by = ? WHERE id = ? AND archived_at IS NULL AND (created_by = ? OR route_to_user_id = ?)');
        } else {
            $stmt = $pdo->prepare('UPDATE activity_logs SET archived_at = NULL, archived_by = NULL, updated_by = ? WHERE id = ? AND archived_at IS NOT NULL AND archived_by = ?');
        }

        foreach ($ids as $id) {
            if ($action === 'archive') {
                if ($isAdmin) {
                    $stmt->execute([$userId, $userId, $id]);
                } else {
                    $stmt->execute([$userId, $userId, $id, $userId, $userId]);
                }
            } else {
                $stmt->execute([$userId, $id, $userId]);
            }

            if ($stmt->rowCount() > 0) {
                $updated++;
                logAudit($action, $id, $details);
            }
        }

        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $e;
    }

    return $updated;
}

function purgeArchivedRecords(PDO $pdo, array $ids): int {
    if (empty($ids)) {
        return 0;
    }

    $deleted = 0;
    $pdo->beginTransaction();
    try {
        $stmt = $pdo->prepare('DELETE FROM activity_logs WHERE id = ? AND archived_at IS NOT NULL');
        foreach ($ids as $id) {
            $stmt->execute([$id]);
            if ($stmt->rowCount() > 0) {
                $deleted++;
                logAudit('delete', $id, 'Archived record permanently deleted');
            }
        }
        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $e;
    }

    return $deleted;
}

if ($method === 'GET') {
    updateLastActivity();
    $scopeAll = isset($_GET['scope']) && $_GET['scope'] === 'all';

    if ($scopeAll) {
        $stmt = $pdo->prepare("
            SELECT al.id, al.municipality, al.lo_claimant, al.title_no, al.odts_no, al.lot_no, al.survey_no,
                   al.area_has, al.location, al.transmitted_documents, al.route_to,
                   al.received_by_control_no, al.remarks_action_taken, al.work_status,
                   al.created_at, al.updated_at, al.archived_at,
                   u1.username AS created_by_name, u2.username AS updated_by_name, u3.username AS archived_by_name
            FROM activity_logs al
            LEFT JOIN users u1 ON al.created_by = u1.id
            LEFT JOIN users u2 ON al.updated_by = u2.id
            LEFT JOIN users u3 ON al.archived_by = u3.id
            WHERE al.archived_at IS NOT NULL
            ORDER BY al.archived_at DESC, al.updated_at DESC
        ");
        $stmt->execute();
    } else {
        $stmt = $pdo->prepare("
            SELECT al.id, al.municipality, al.lo_claimant, al.title_no, al.odts_no, al.lot_no, al.survey_no,
                   al.area_has, al.location, al.transmitted_documents, al.route_to,
                   al.received_by_control_no, al.remarks_action_taken, al.work_status,
                   al.created_at, al.updated_at, al.archived_at,
                   u1.username AS created_by_name, u2.username AS updated_by_name, u3.username AS archived_by_name
            FROM activity_logs al
            LEFT JOIN users u1 ON al.created_by = u1.id
            LEFT JOIN users u2 ON al.updated_by = u2.id
            LEFT JOIN users u3 ON al.archived_by = u3.id
            WHERE al.archived_at IS NOT NULL
              AND al.archived_by = ?
            ORDER BY al.archived_at DESC, al.updated_at DESC
        ");
        $stmt->execute([$userId]);
    }

    echo json_encode(['success' => true, 'data' => $stmt->fetchAll()]);
    exit;
}

if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $action = $input['action'] ?? 'archive';
    $ids = normalizeIdList($input['ids'] ?? ($input['id'] ?? null));
    $isAdmin = isAdmin();

    if (!in_array($action, ['archive', 'restore', 'purge_all'], true)) {
        echo json_encode(['success' => false, 'message' => 'Invalid archive action']);
        exit;
    }

    try {
        if ($action === 'purge_all') {
            if (!isAdmin()) {
                http_response_code(403);
                echo json_encode(['success' => false, 'message' => 'Admin only']);
                exit;
            }
            $archiveIds = fetchArchivedIds($pdo, $userId);
            if (empty($archiveIds)) {
                echo json_encode(['success' => true, 'message' => 'Archive is already empty', 'updated' => 0]);
                exit;
            }
            $updated = purgeArchivedRecords($pdo, $archiveIds);
        } else {
            if (empty($ids)) {
                echo json_encode(['success' => false, 'message' => 'No record IDs provided']);
                exit;
            }
            $updated = applyArchiveAction($pdo, $ids, $action, $userId, $isAdmin);
        }
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Unable to update archive state']);
        exit;
    }

    if ($updated < 1) {
        echo json_encode(['success' => false, 'message' => $action === 'archive' ? 'Oops! You can only archive records that you created.' : ($action === 'restore' ? 'No archived records were restored' : 'No archived records were deleted')]);
        exit;
    }

    echo json_encode([
        'success' => true,
        'message' => $action === 'archive'
            ? $updated . ' record(s) archived'
            : ($action === 'restore' ? $updated . ' record(s) restored' : $updated . ' archived record(s) permanently deleted'),
        'updated' => $updated,
    ]);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'message' => 'Method not allowed']);

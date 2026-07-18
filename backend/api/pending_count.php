<?php
/**
 * DAR Activity Logs - Pending Records Count API
 *
 * Returns the total number of pending (not_finished, not archived) records
 * visible to the current user. Admins see all users; regular users see only
 * their own records.
 */

require_once __DIR__ . '/../includes/init.php';

header('Content-Type: application/json');
requireLogin();

$pdo    = getDB();
$user   = currentUser();
$userId = (int) $user['id'];
$isAdmin = isAdmin();

try {
    if ($isAdmin) {
        $stmt = $pdo->query(
            "SELECT COUNT(*) AS cnt
               FROM activity_logs
              WHERE archived_at IS NULL
                AND work_status = 'not_finished'"
        );
    } else {
        $stmt = $pdo->prepare(
            "SELECT COUNT(*) AS cnt
               FROM activity_logs
              WHERE archived_at IS NULL
                AND work_status = 'not_finished'
                AND created_by = ?"
        );
        $stmt->execute([$userId]);
    }

    $row   = $stmt->fetch();
    $count = $row ? (int) $row['cnt'] : 0;

    echo json_encode(['success' => true, 'count' => $count]);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'count' => 0]);
}

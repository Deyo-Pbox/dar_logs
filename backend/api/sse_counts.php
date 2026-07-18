<?php
/**
 * DAR Activity Logs - Server-Sent Events: Live Badge Counts + New Notification Alert
 *
 * InfinityFree-compatible: max 10-second stream (free-tier execution limit).
 * Reconnects automatically via EventSource browser retry.
 *
 * Events emitted:
 *   "counts"  -> { notif, pending, new_notif?: { id, message, sender_name, record_id } }
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/notifications.php';

requireLogin();
$user    = currentUser();
$userId  = (int) $user['id'];
$isAdmin = isAdmin();

session_write_close();

header('Content-Type: text/event-stream');
header('Cache-Control: no-cache, no-store');
header('X-Accel-Buffering: no');
header('Connection: keep-alive');

while (ob_get_level()) {
    ob_end_flush();
}
flush();

function getNotifData(PDO $pdo, int $userId): array {
    $stmt = $pdo->prepare(
        "SELECT n.id, n.message, n.record_id, u.username AS sender_name
           FROM notifications n
           LEFT JOIN users u ON n.sender_id = u.id
          WHERE n.user_id = ? AND n.is_read = 0
          ORDER BY n.created_at DESC, n.id DESC
          LIMIT 1"
    );
    $stmt->execute([$userId]);
    $newest = $stmt->fetch(PDO::FETCH_ASSOC) ?: null;

    $countStmt = $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0");
    $countStmt->execute([$userId]);
    $count = (int) $countStmt->fetchColumn();

    return ['count' => $count, 'newest' => $newest];
}

function getPendingCount(PDO $pdo, int $userId, bool $isAdmin): int {
    if ($isAdmin) {
        $stmt = $pdo->query(
            "SELECT COUNT(*) FROM activity_logs
              WHERE archived_at IS NULL AND work_status = 'not_finished'"
        );
    } else {
        $stmt = $pdo->prepare(
            "SELECT COUNT(*) FROM activity_logs
              WHERE archived_at IS NULL
                AND work_status = 'not_finished'
                AND created_by = ?"
        );
        $stmt->execute([$userId]);
    }
    return (int) $stmt->fetchColumn();
}

$pdo          = getDB();
$prevNotif    = -1;
$prevPending  = -1;
$prevNewestId = null;

ensureNotificationsSchema($pdo);
trimNotificationsForUser($pdo, $userId);

// InfinityFree free tier: 10-second max, one poll iteration
$deadline = time() + 9;

try {
    // Emit at least one update so the client gets fresh counts
    $notifData = getNotifData($pdo, $userId);
    $notif     = $notifData['count'];
    $newest    = $notifData['newest'];
    $pending   = getPendingCount($pdo, $userId, $isAdmin);
    $newestId  = $newest ? (int) $newest['id'] : null;

    $payload = ['notif' => $notif, 'pending' => $pending];

    if ($notif > 0 && $newest) {
        $payload['new_notif'] = [
            'id'          => (int)    $newest['id'],
            'message'     => (string) $newest['message'],
            'sender_name' => (string) ($newest['sender_name'] ?? 'Someone'),
            'record_id'   => $newest['record_id'] !== null ? (int) $newest['record_id'] : null,
        ];
    }

    echo "event: counts\n";
    echo "data: " . json_encode($payload) . "\n\n";
    flush();

    // Try a second poll if time permits
    if (time() < $deadline && !connection_aborted()) {
        sleep(4);

        $notifData2 = getNotifData($pdo, $userId);
        $notif2     = $notifData2['count'];
        $newest2    = $notifData2['newest'];
        $pending2   = getPendingCount($pdo, $userId, $isAdmin);

        if ($notif2 !== $notif || $pending2 !== $pending) {
            $payload2 = ['notif' => $notif2, 'pending' => $pending2];
            $newestId2 = $newest2 ? (int) $newest2['id'] : null;
            if ($newest2 && $newestId2 !== $newestId) {
                $payload2['new_notif'] = [
                    'id'          => (int)    $newest2['id'],
                    'message'     => (string) $newest2['message'],
                    'sender_name' => (string) ($newest2['sender_name'] ?? 'Someone'),
                    'record_id'   => $newest2['record_id'] !== null ? (int) $newest2['record_id'] : null,
                ];
            }
            echo "event: counts\n";
            echo "data: " . json_encode($payload2) . "\n\n";
            flush();
        }
    }

    // Tell browser to reconnect quickly
    echo "retry: 3000\n\n";
    flush();
} catch (Exception $e) {
    echo "retry: 5000\n\n";
    flush();
}

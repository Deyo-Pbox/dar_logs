<?php
/**
 * DAR Activity Logs - Notifications schema helpers
 */

const DAR_NOTIFICATION_HISTORY_LIMIT = 10;

function ensureNotificationsSchema(PDO $pdo): void {
    static $checked = false;
    if ($checked) {
        return;
    }
    $checked = true;

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS notifications (
            id INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            user_id INT(11) UNSIGNED NOT NULL,
            type VARCHAR(32) NOT NULL DEFAULT 'route',
            record_id INT(11) UNSIGNED DEFAULT NULL,
            sender_id INT(11) UNSIGNED DEFAULT NULL,
            message VARCHAR(255) NOT NULL,
            is_read TINYINT(1) NOT NULL DEFAULT 0,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id),
            KEY idx_notif_user_id (user_id),
            KEY idx_notif_is_read (is_read),
            KEY idx_notif_created_at (created_at),
            KEY idx_notif_record_id (record_id),
            KEY idx_notif_unread (user_id, is_read),
            KEY idx_notif_user_latest (user_id, created_at),
            CONSTRAINT notifications_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
            CONSTRAINT notifications_sender_id FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE SET NULL,
            CONSTRAINT notifications_record_id FOREIGN KEY (record_id) REFERENCES activity_logs (id) ON DELETE SET NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    ");

    ensureNotificationsIndexes($pdo);
}

function ensureNotificationsIndexes(PDO $pdo): void
{
    static $indexChecked = false;
    if ($indexChecked) return;
    $indexChecked = true;

    $existing = [];
    $rows = $pdo->query('SHOW INDEX FROM notifications')->fetchAll();
    foreach ($rows as $row) {
        $existing[$row['Key_name']] = true;
    }

    if (!isset($existing['idx_notif_unread'])) {
        $pdo->exec('ALTER TABLE notifications ADD INDEX idx_notif_unread (user_id, is_read)');
    }
    if (!isset($existing['idx_notif_user_latest'])) {
        $pdo->exec('ALTER TABLE notifications ADD INDEX idx_notif_user_latest (user_id, created_at)');
    }
}

function trimNotificationsForUser(PDO $pdo, int $userId, int $keep = DAR_NOTIFICATION_HISTORY_LIMIT): void {
    $keep = max(1, (int) $keep);

    $stmt = $pdo->prepare("
        DELETE FROM notifications
        WHERE user_id = ?
          AND id NOT IN (
              SELECT id
              FROM (
                  SELECT id
                  FROM notifications
                  WHERE user_id = ?
                  ORDER BY created_at DESC, id DESC
                  LIMIT $keep
              ) AS recent_notifications
          )
    ");
    $stmt->execute([$userId, $userId]);
}

function createRouteNotification(PDO $pdo, int $receiverUserId, int $senderUserId, int $recordId, string $senderUsername): void {
    $msg = 'Record routed from ' . $senderUsername . '.';
    $stmt = $pdo->prepare('INSERT INTO notifications (user_id, type, record_id, sender_id, message) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute([$receiverUserId, 'route', $recordId, $senderUserId, $msg]);
    trimNotificationsForUser($pdo, $receiverUserId);
}

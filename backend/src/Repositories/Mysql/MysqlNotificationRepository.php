<?php

declare(strict_types=1);

namespace DarLogs\Repositories\Mysql;

use DarLogs\Core\Database;
use DarLogs\Models\Notification;
use DarLogs\Repositories\NotificationRepository;
use PDO;

class MysqlNotificationRepository implements NotificationRepository
{
    private PDO $pdo;

    public function __construct()
    {
        $this->pdo = Database::getInstance();
    }

    public function findByUser(int $userId, int $limit = 50): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT n.id, n.user_id, n.type, n.record_id, n.sender_id, n.message, n.is_read, n.created_at,
                    u.username AS sender_name
            FROM notifications n
            LEFT JOIN users u ON n.sender_id = u.id
            WHERE n.user_id = ?
            ORDER BY n.created_at DESC, n.id DESC LIMIT ?'
        );
        $stmt->execute([$userId, $limit]);
        return array_map([$this, 'hydrate'], $stmt->fetchAll());
    }

    public function countUnread(int $userId): int
    {
        $stmt = $this->pdo->prepare('SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0');
        $stmt->execute([$userId]);
        return (int) $stmt->fetchColumn();
    }

    public function markRead(int $id, int $userId): bool
    {
        $stmt = $this->pdo->prepare('UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?');
        $stmt->execute([$id, $userId]);
        return $stmt->rowCount() > 0;
    }

    public function markAllRead(int $userId): int
    {
        $stmt = $this->pdo->prepare('UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0');
        $stmt->execute([$userId]);
        return $stmt->rowCount();
    }

    public function create(int $userId, string $type, ?int $recordId, ?int $senderId, string $message): void
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO notifications (user_id, type, record_id, sender_id, message) VALUES (?, ?, ?, ?, ?)'
        );
        $stmt->execute([$userId, $type, $recordId, $senderId, $message]);
    }

    public function trimForUser(int $userId, int $keep = 50): void
    {
        $keep = max(1, $keep);
        $stmt = $this->pdo->prepare(
            'DELETE FROM notifications WHERE user_id = ? AND id NOT IN ('
            . 'SELECT id FROM (SELECT id FROM notifications WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ' . (int) $keep
            . ') AS recent)'
        );
        $stmt->execute([$userId, $userId]);
    }

    public function ensureSchema(): void
    {
        static $checked = false;
        if ($checked) return;
        $checked = true;

        $this->pdo->exec("CREATE TABLE IF NOT EXISTS notifications (
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
            KEY idx_notif_sender_id (sender_id),
            CONSTRAINT notifications_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
            CONSTRAINT notifications_sender_id FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE SET NULL,
            CONSTRAINT notifications_record_id FOREIGN KEY (record_id) REFERENCES activity_logs (id) ON DELETE SET NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        $this->ensureIndexes();
    }

    private function ensureIndexes(): void
    {
        $existing = [];
        $rows = $this->pdo->query('SHOW INDEX FROM notifications')->fetchAll();
        foreach ($rows as $row) {
            $existing[$row['Key_name']] = true;
        }

        $indexes = [
            'idx_notif_sender_id'    => 'ALTER TABLE notifications ADD INDEX idx_notif_sender_id (sender_id)',
            'idx_notif_unread'       => 'ALTER TABLE notifications ADD INDEX idx_notif_unread (user_id, is_read)',
            'idx_notif_user_latest'  => 'ALTER TABLE notifications ADD INDEX idx_notif_user_latest (user_id, created_at)',
        ];

        foreach ($indexes as $name => $sql) {
            if (!isset($existing[$name])) {
                $this->pdo->exec($sql);
            }
        }
    }

    private function hydrate(array $row): Notification
    {
        $n = new Notification();
        $n->id = (int) $row['id'];
        $n->userId = (int) $row['user_id'];
        $n->type = $row['type'];
        $n->recordId = isset($row['record_id']) ? (int) $row['record_id'] : null;
        $n->senderId = isset($row['sender_id']) ? (int) $row['sender_id'] : null;
        $n->message = $row['message'];
        $n->isRead = (bool) $row['is_read'];
        $n->createdAt = $row['created_at'] ?? null;
        return $n;
    }
}

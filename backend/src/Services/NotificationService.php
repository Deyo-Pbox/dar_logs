<?php

declare(strict_types=1);

namespace DarLogs\Services;

use DarLogs\Config\AppConfig;
use DarLogs\Repositories\Mysql\MysqlNotificationRepository;
use DarLogs\Repositories\NotificationRepository;

class NotificationService
{
    private NotificationRepository $notifRepo;

    public function __construct()
    {
        $this->notifRepo = new MysqlNotificationRepository();
        $this->notifRepo->ensureSchema();
    }

    public function listForUser(int $userId): array
    {
        $cap = (int) AppConfig::get('NOTIFICATION_CAP', 50);
        $this->notifRepo->trimForUser($userId, $cap);
        return $this->notifRepo->findByUser($userId, $cap);
    }

    public function countUnread(int $userId): int
    {
        return $this->notifRepo->countUnread($userId);
    }

    public function markRead(int $id, int $userId): void
    {
        $this->notifRepo->markRead($id, $userId);
    }

    public function markAllRead(int $userId): int
    {
        return $this->notifRepo->markAllRead($userId);
    }

    public function streamCounts(int $userId): void
    {
        header('Content-Type: text/event-stream');
        header('Cache-Control: no-cache');
        header('Connection: keep-alive');
        header('X-Accel-Buffering: no');

        $maxRuntime = 240;
        $started = time();

        while ((time() - $started) < $maxRuntime) {
            $notifCount = $this->notifRepo->countUnread($userId);

            $payload = [
                'notif_count'   => $notifCount,
                'pending_count' => 0, // populated by separate query if needed
                'timestamp'     => date('c'),
            ];

            echo "event: counts\ndata: " . json_encode($payload) . "\n\n";
            ob_flush();
            flush();

            sleep(4);
        }

        echo "retry: 1000\n\n";
        ob_flush();
        flush();
    }
}

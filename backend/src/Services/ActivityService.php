<?php

declare(strict_types=1);

namespace DarLogs\Services;

use DarLogs\Config\AppConfig;
use DarLogs\Core\Cache;
use DarLogs\Core\Database;
use DarLogs\Exceptions\NotFoundException;
use DarLogs\Repositories\ActivityRepository;
use DarLogs\Repositories\AuditRepository;
use DarLogs\Repositories\Mysql\MysqlActivityRepository;
use DarLogs\Repositories\Mysql\MysqlAuditRepository;
use DarLogs\Repositories\Mysql\MysqlUserRepository;
use DarLogs\Repositories\NotificationRepository;
use DarLogs\Repositories\Mysql\MysqlNotificationRepository;
use DarLogs\Repositories\UserRepository;
use PDO;

class ActivityService
{
    private ActivityRepository $activityRepo;
    private AuditRepository $auditRepo;
    private NotificationRepository $notifRepo;
    private UserRepository $userRepo;

    public function __construct()
    {
        $this->activityRepo = new MysqlActivityRepository();
        $this->auditRepo = new MysqlAuditRepository();
        $this->notifRepo = new MysqlNotificationRepository();
        $this->userRepo = new MysqlUserRepository();

        $this->activityRepo->ensureSchema();
        $this->auditRepo->ensureSchema();
        $this->notifRepo->ensureSchema();
    }

    private function invalidateStats(): void
    {
        Cache::forget('dashboard_stats');
        Cache::forget('pending_count');
    }

    /** @return array{records: array, total: int} */
    public function listActive(array $filters = [], int $page = 1, int $perPage = 20): array
    {
        return [
            'records' => $this->activityRepo->findAll($filters, $page, $perPage),
            'total'   => $this->activityRepo->countAll($filters),
            'page'    => $page,
            'per_page'=> $perPage,
        ];
    }

    public function getById(int $id): mixed
    {
        $record = $this->activityRepo->findById($id);
        if ($record === null) {
            throw new NotFoundException('Record not found');
        }
        return $record;
    }

    public function create(array $data, object $currentUser): int
    {
        $pdo = Database::getInstance();
        $pdo->beginTransaction();

        try {
            $routeToUserId = (int) ($data['route_to_user_id'] ?? 0);
            $routeToUserName = '';

            if ($routeToUserId > 0) {
                $target = $this->userRepo->findById($routeToUserId);
                if ($target !== null && $target->approved) {
                    $routeToUserName = $target->username;
                } else {
                    $routeToUserId = 0;
                }
            }

            $fields = [
                'municipality'           => $data['municipality'],
                'lo_claimant'            => $data['lo_claimant'],
                'title_no'               => $data['title_no'],
                'odts_no'                => $data['odts_no'],
                'lot_no'                 => $data['lot_no'],
                'survey_no'              => $data['survey_no'],
                'area_has'               => $data['area_has'],
                'location'               => $data['location'],
                'transmitted_documents'  => $data['transmitted_documents'],
                'route_to'               => $routeToUserName !== '' ? $routeToUserName : $data['route_to'],
                'route_to_user_id'       => $routeToUserId > 0 ? $routeToUserId : null,
                'routed_from_user_id'    => $routeToUserId > 0 ? $currentUser->id : null,
                'routed_at'              => $routeToUserId > 0 ? date('Y-m-d H:i:s') : null,
                'received_by_control_no' => $data['received_by_control_no'],
                'remarks_action_taken'   => $data['remarks_action_taken'],
                'work_status'            => $data['work_status'],
                'created_by'             => $currentUser->id,
                'updated_by'             => $currentUser->id,
            ];

            $id = $this->activityRepo->create($fields);

            $this->auditRepo->log($currentUser->id, $currentUser->username, 'add', 'activity_logs', $id, 'Record created');
            $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));

            if ($routeToUserId > 0) {
                $this->notifRepo->create(
                    $routeToUserId, 'route', $id, $currentUser->id,
                    'Record routed from ' . $currentUser->username . '.'
                );
                $this->notifRepo->trimForUser($routeToUserId, (int) AppConfig::get('NOTIFICATION_CAP', 50));
            }

            $pdo->commit();
        } catch (\Throwable $e) {
            $pdo->rollBack();
            throw $e;
        }

        $this->invalidateStats();
        return $id;
    }

    public function update(int $id, array $data, object $currentUser): void
    {
        $record = $this->activityRepo->findById($id)
            ?? throw new NotFoundException('Record not found');

        if ($record->archivedAt !== null) {
            throw new NotFoundException('Archived records must be restored before editing');
        }

        $routeToUserId = (int) ($data['route_to_user_id'] ?? 0);
        $routeToUserName = '';

        if ($routeToUserId > 0) {
            $target = $this->userRepo->findById($routeToUserId);
            if ($target !== null && $target->approved) {
                $routeToUserName = $target->username;
            } else {
                $routeToUserId = 0;
            }
        }

        $fields = [
            'municipality'           => $data['municipality'],
            'lo_claimant'            => $data['lo_claimant'],
            'title_no'               => $data['title_no'],
            'odts_no'                => $data['odts_no'],
            'lot_no'                 => $data['lot_no'],
            'survey_no'              => $data['survey_no'],
            'area_has'               => $data['area_has'],
            'location'               => $data['location'],
            'transmitted_documents'  => $data['transmitted_documents'],
            'route_to'               => $routeToUserName !== '' ? $routeToUserName : $data['route_to'],
            'route_to_user_id'       => $routeToUserId > 0 ? $routeToUserId : null,
            'routed_from_user_id'    => $routeToUserId > 0 ? $currentUser->id : null,
            'routed_at'              => $routeToUserId > 0 ? date('Y-m-d H:i:s') : null,
            'received_by_control_no' => $data['received_by_control_no'],
            'remarks_action_taken'   => $data['remarks_action_taken'],
            'work_status'            => $data['work_status'],
            'updated_by'             => $currentUser->id,
        ];

        $prevRouteToUserId = $record->routeToUserId ?? 0;
        $routeChanged = $routeToUserId > 0 && $routeToUserId !== $prevRouteToUserId;

        $pdo = Database::getInstance();
        $pdo->beginTransaction();

        try {
            $this->activityRepo->update($id, $fields);

            $this->auditRepo->log($currentUser->id, $currentUser->username, 'edit', 'activity_logs', $id, 'Record updated');
            $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));

            if ($routeChanged) {
                $this->notifRepo->create(
                    $routeToUserId, 'route', $id, $currentUser->id,
                    'Record routed from ' . $currentUser->username . '.'
                );
                $this->notifRepo->trimForUser($routeToUserId, (int) AppConfig::get('NOTIFICATION_CAP', 50));
            }

            $pdo->commit();
        } catch (\Throwable $e) {
            $pdo->rollBack();
            throw $e;
        }

        $this->invalidateStats();
    }

    public function archive(int $id, object $currentUser): void
    {
        $record = $this->activityRepo->findById($id)
            ?? throw new NotFoundException('Record not found');

        if ($record->archivedAt !== null) {
            throw new NotFoundException('Record is already archived');
        }

        $this->activityRepo->archive($id, $currentUser->id);
        $this->auditRepo->log($currentUser->id, $currentUser->username, 'archive', 'activity_logs', $id, 'Record archived');
        $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));
        $this->invalidateStats();
    }

    public function restore(int $id, object $currentUser): void
    {
        $record = $this->activityRepo->findById($id)
            ?? throw new NotFoundException('Record not found');

        if ($record->archivedAt === null) {
            throw new NotFoundException('Record is not archived');
        }

        $this->activityRepo->restore($id);
        $this->auditRepo->log($currentUser->id, $currentUser->username, 'restore', 'activity_logs', $id, 'Record restored');
        $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));
        $this->invalidateStats();
    }

    public function delete(int $id, object $currentUser): void
    {
        $record = $this->activityRepo->findById($id)
            ?? throw new NotFoundException('Record not found');

        if ($record->archivedAt !== null) {
            throw new NotFoundException('Archived records must be restored before deleting');
        }

        $this->activityRepo->delete($id);
        $this->auditRepo->log($currentUser->id, $currentUser->username, 'delete', 'activity_logs', $id, 'Record deleted');
        $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));
        $this->invalidateStats();
    }

    public function route(int $id, int $targetUserId, object $currentUser): void
    {
        $record = $this->activityRepo->findById($id)
            ?? throw new NotFoundException('Record not found');

        if ($record->archivedAt !== null) {
            throw new NotFoundException('Cannot route an archived record');
        }

        $target = $this->userRepo->findById($targetUserId);
        if ($target === null || !$target->approved) {
            throw new NotFoundException('Target user not found or not approved');
        }

        $this->activityRepo->routeToUser($id, $targetUserId, $currentUser->id);

        $this->notifRepo->create(
            $targetUserId, 'route', $id, $currentUser->id,
            'Record routed from ' . $currentUser->username . '.'
        );
        $this->notifRepo->trimForUser($targetUserId, (int) AppConfig::get('NOTIFICATION_CAP', 50));

        $this->auditRepo->log($currentUser->id, $currentUser->username, 'edit', 'activity_logs', $id, 'Record routed to ' . $target->username);
        $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));
        $this->invalidateStats();
    }

    public function getArchived(int $page = 1, int $perPage = 20): array
    {
        return [
            'records' => $this->activityRepo->findArchived($page, $perPage),
            'total'   => $this->activityRepo->countArchived(),
            'page'    => $page,
            'per_page'=> $perPage,
        ];
    }

    public function getDashboardStats(): array
    {
        return Cache::remember('dashboard_stats', 30, fn() => $this->activityRepo->getDashboardStats());
    }

    public function getPendingCount(?int $userId = null): int
    {
        $key = 'pending_count_' . ($userId ?? 'all');
        return Cache::remember($key, 15, fn() => $this->activityRepo->countPending($userId));
    }

    public function getAuditLog(int $limit = 100): array
    {
        $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));
        return $this->auditRepo->findAll($limit);
    }
}

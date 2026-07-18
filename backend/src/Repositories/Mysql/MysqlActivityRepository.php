<?php

declare(strict_types=1);

namespace DarLogs\Repositories\Mysql;

use DarLogs\Core\Database;
use DarLogs\Models\ActivityLog;
use DarLogs\Repositories\ActivityRepository;
use PDO;

class MysqlActivityRepository implements ActivityRepository
{
    private PDO $pdo;

    public function __construct()
    {
        $this->pdo = Database::getInstance();
    }

    private const BASE_COLUMNS = 'al.id, al.municipality, al.lo_claimant, al.title_no, al.odts_no, al.lot_no,
        al.survey_no, al.area_has, al.location, al.transmitted_documents, al.route_to,
        al.route_to_user_id, al.routed_from_user_id, al.routed_at,
        al.received_by_control_no, al.remarks_action_taken, al.work_status,
        al.archived_at, al.archived_by, al.created_by, al.updated_by,
        al.created_at, al.updated_at,
        u1.username AS created_by_name, u2.username AS updated_by_name,
        u3.username AS routed_from_name, u4.username AS route_to_username';

    private const BASE_JOINS = 'LEFT JOIN users u1 ON al.created_by = u1.id
        LEFT JOIN users u2 ON al.updated_by = u2.id
        LEFT JOIN users u3 ON al.routed_from_user_id = u3.id
        LEFT JOIN users u4 ON al.route_to_user_id = u4.id';

    public function findAll(array $filters = [], int $page = 1, int $perPage = 20): array
    {
        $where = ['al.archived_at IS NULL'];
        $params = [];

        if (!empty($filters['municipality'])) {
            $where[] = 'al.municipality = ?';
            $params[] = $filters['municipality'];
        }
        if (!empty($filters['work_status'])) {
            $where[] = 'al.work_status = ?';
            $params[] = $filters['work_status'];
        }
        if (!empty($filters['search'])) {
            $term = trim($filters['search']);
            if (mb_strlen($term) >= 4) {
                $where[] = 'MATCH(al.lo_claimant, al.title_no, al.lot_no, al.received_by_control_no) AGAINST(? IN BOOLEAN MODE)';
                $params[] = $term;
            } else {
                $where[] = '(al.lo_claimant LIKE ? OR al.title_no LIKE ? OR al.lot_no LIKE ? OR al.received_by_control_no LIKE ?)';
                $s = '%' . $term . '%';
                $params = array_merge($params, [$s, $s, $s, $s]);
            }
        }
        if (!empty($filters['user_id'])) {
            $where[] = '(al.created_by = ? OR al.route_to_user_id = ?)';
            $params[] = $filters['user_id'];
            $params[] = $filters['user_id'];
        }

        $offset = max(0, ($page - 1) * $perPage);
        $sql = 'SELECT ' . self::BASE_COLUMNS . ' FROM activity_logs al ' . self::BASE_JOINS
            . ' WHERE ' . implode(' AND ', $where)
            . ' ORDER BY al.updated_at DESC LIMIT ' . (int) $perPage . ' OFFSET ' . (int) $offset;

        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return array_map([$this, 'hydrate'], $stmt->fetchAll());
    }

    public function countAll(array $filters = []): int
    {
        $where = ['al.archived_at IS NULL'];
        $params = [];

        if (!empty($filters['municipality'])) {
            $where[] = 'al.municipality = ?';
            $params[] = $filters['municipality'];
        }
        if (!empty($filters['work_status'])) {
            $where[] = 'al.work_status = ?';
            $params[] = $filters['work_status'];
        }
        if (!empty($filters['search'])) {
            $term = trim($filters['search']);
            if (mb_strlen($term) >= 4) {
                $where[] = 'MATCH(al.lo_claimant, al.title_no, al.lot_no) AGAINST(? IN BOOLEAN MODE)';
                $params[] = $term;
            } else {
                $where[] = '(al.lo_claimant LIKE ? OR al.title_no LIKE ? OR al.lot_no LIKE ?)';
                $s = '%' . $term . '%';
                $params = array_merge($params, [$s, $s, $s]);
            }
        }
        if (!empty($filters['user_id'])) {
            $where[] = '(al.created_by = ? OR al.route_to_user_id = ?)';
            $params[] = $filters['user_id'];
            $params[] = $filters['user_id'];
        }

        $stmt = $this->pdo->prepare('SELECT COUNT(*) FROM activity_logs al WHERE ' . implode(' AND ', $where));
        $stmt->execute($params);
        return (int) $stmt->fetchColumn();
    }

    public function findById(int $id): ?ActivityLog
    {
        $stmt = $this->pdo->prepare(
            'SELECT ' . self::BASE_COLUMNS . ' FROM activity_logs al ' . self::BASE_JOINS . ' WHERE al.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ? $this->hydrate($row) : null;
    }

    public function findByUser(int $userId): array
    {
        return $this->findAll(['user_id' => $userId]);
    }

    public function findArchived(int $page = 1, int $perPage = 20): array
    {
        $offset = max(0, ($page - 1) * $perPage);
        $stmt = $this->pdo->prepare(
            'SELECT ' . self::BASE_COLUMNS . ', u5.username AS archived_by_name
            FROM activity_logs al ' . self::BASE_JOINS . '
            LEFT JOIN users u5 ON al.archived_by = u5.id
            WHERE al.archived_at IS NOT NULL
            ORDER BY al.archived_at DESC LIMIT ? OFFSET ?'
        );
        $stmt->execute([$perPage, $offset]);
        return array_map([$this, 'hydrate'], $stmt->fetchAll());
    }

    public function countArchived(): int
    {
        return (int) $this->pdo->query('SELECT COUNT(*) FROM activity_logs WHERE archived_at IS NOT NULL')->fetchColumn();
    }

    public function countPending(?int $userId = null): int
    {
        if ($userId !== null) {
            $stmt = $this->pdo->prepare(
                'SELECT COUNT(*) FROM activity_logs WHERE work_status = ? AND archived_at IS NULL AND created_by = ?'
            );
            $stmt->execute(['not_finished', $userId]);
            return (int) $stmt->fetchColumn();
        }
        return (int) $this->pdo->query(
            "SELECT COUNT(*) FROM activity_logs WHERE work_status = 'not_finished' AND archived_at IS NULL"
        )->fetchColumn();
    }

    public function countCompleted(): int
    {
        return (int) $this->pdo->query(
            "SELECT COUNT(*) FROM activity_logs WHERE work_status = 'finished' AND archived_at IS NULL"
        )->fetchColumn();
    }

    public function findPending(int $page = 1, int $perPage = 20): array
    {
        return $this->findAll(['work_status' => 'not_finished'], $page, $perPage);
    }

    public function findCompleted(int $page = 1, int $perPage = 20): array
    {
        return $this->findAll(['work_status' => 'finished'], $page, $perPage);
    }

    public function findRecentEdits(int $limit = 5): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT ' . self::BASE_COLUMNS . ' FROM activity_logs al ' . self::BASE_JOINS
            . ' ORDER BY al.updated_at DESC LIMIT ?'
        );
        $stmt->execute([$limit]);
        return array_map([$this, 'hydrate'], $stmt->fetchAll());
    }

    public function create(array $fields): int
    {
        $cols = [
            'municipality', 'lo_claimant', 'title_no', 'odts_no', 'lot_no', 'survey_no',
            'area_has', 'location', 'transmitted_documents', 'route_to',
            'route_to_user_id', 'routed_from_user_id', 'routed_at',
            'received_by_control_no', 'remarks_action_taken', 'work_status',
            'created_by', 'updated_by',
        ];

        $placeholders = implode(',', array_fill(0, count($cols), '?'));
        $stmt = $this->pdo->prepare(
            'INSERT INTO activity_logs (' . implode(',', $cols) . ') VALUES (' . $placeholders . ')'
        );
        $values = [];
        foreach ($cols as $col) {
            $values[] = $fields[$col] ?? null;
        }
        $stmt->execute($values);
        return (int) $this->pdo->lastInsertId();
    }

    public function update(int $id, array $fields): bool
    {
        $allowed = [
            'municipality', 'lo_claimant', 'title_no', 'odts_no', 'lot_no', 'survey_no',
            'area_has', 'location', 'transmitted_documents', 'route_to',
            'route_to_user_id', 'routed_from_user_id', 'routed_at',
            'received_by_control_no', 'remarks_action_taken', 'work_status', 'updated_by',
        ];

        $sets = [];
        $params = [];
        foreach ($allowed as $col) {
            if (array_key_exists($col, $fields)) {
                $sets[] = "`{$col}` = ?";
                $params[] = $fields[$col];
            }
        }
        if (empty($sets)) {
            return false;
        }
        $params[] = $id;
        $stmt = $this->pdo->prepare('UPDATE activity_logs SET ' . implode(', ', $sets) . ' WHERE id = ?');
        $stmt->execute($params);
        return $stmt->rowCount() > 0;
    }

    public function archive(int $id, int $archivedBy): bool
    {
        $stmt = $this->pdo->prepare(
            'UPDATE activity_logs SET archived_at = NOW(), archived_by = ?, updated_by = ? WHERE id = ? AND archived_at IS NULL'
        );
        $stmt->execute([$archivedBy, $archivedBy, $id]);
        return $stmt->rowCount() > 0;
    }

    public function restore(int $id): bool
    {
        $stmt = $this->pdo->prepare(
            'UPDATE activity_logs SET archived_at = NULL, archived_by = NULL WHERE id = ? AND archived_at IS NOT NULL'
        );
        $stmt->execute([$id]);
        return $stmt->rowCount() > 0;
    }

    public function delete(int $id): bool
    {
        $stmt = $this->pdo->prepare('DELETE FROM activity_logs WHERE id = ? AND archived_at IS NULL');
        $stmt->execute([$id]);
        return $stmt->rowCount() > 0;
    }

    public function routeToUser(int $id, int $routeToUserId, int $routedFromUserId): bool
    {
        $stmt = $this->pdo->prepare(
            'UPDATE activity_logs SET route_to_user_id = ?, routed_from_user_id = ?, routed_at = NOW() WHERE id = ?'
        );
        $stmt->execute([$routeToUserId, $routedFromUserId, $id]);
        return $stmt->rowCount() > 0;
    }

    public function getDashboardStats(): array
    {
        $row = $this->pdo->query(
            'SELECT
                COALESCE(SUM(archived_at IS NULL), 0) AS total_records,
                COALESCE(SUM(archived_at IS NOT NULL), 0) AS archived_records
            FROM activity_logs'
        )->fetch();

        $userStats = $this->pdo->query(
            'SELECT
                COUNT(*) AS total_users,
                SUM(last_activity >= DATE_SUB(NOW(), INTERVAL 15 MINUTE) AND approved = 1) AS active_users
            FROM users'
        )->fetch();

        $editCount = $this->pdo->query(
            "SELECT COUNT(*) FROM audit_log
            WHERE action IN ('add','edit','delete','archive','restore')
            AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)"
        )->fetchColumn();

        return [
            'total_records'    => (int) $row['total_records'],
            'archived_records' => (int) $row['archived_records'],
            'total_users'      => (int) $userStats['total_users'],
            'recent_edits'     => (int) $editCount,
            'active_users'     => (int) $userStats['active_users'],
        ];
    }

    public function ensureSchema(): void
    {
        static $checked = false;
        if ($checked) return;

        $stampPath = __DIR__ . '/../../../storage/schema_checked_activity_logs';
        if (file_exists($stampPath) && filemtime($stampPath) > time() - 3600) {
            $checked = true;
            return;
        }
        $checked = true;

        $migrations = [
            ['name' => 'work_status', 'sql' => "ALTER TABLE activity_logs ADD COLUMN work_status ENUM('not_finished','finished') NOT NULL DEFAULT 'not_finished' AFTER remarks_action_taken"],
            ['name' => 'archived_at', 'sql' => 'ALTER TABLE activity_logs ADD COLUMN archived_at DATETIME NULL AFTER work_status'],
            ['name' => 'archived_by', 'sql' => 'ALTER TABLE activity_logs ADD COLUMN archived_by INT UNSIGNED NULL AFTER archived_at'],
            ['name' => 'odts_no', 'sql' => 'ALTER TABLE activity_logs ADD COLUMN odts_no VARCHAR(128) NULL AFTER title_no'],
            ['name' => 'route_to_user_id', 'sql' => 'ALTER TABLE activity_logs ADD COLUMN route_to_user_id INT UNSIGNED NULL AFTER route_to'],
            ['name' => 'routed_from_user_id', 'sql' => 'ALTER TABLE activity_logs ADD COLUMN routed_from_user_id INT UNSIGNED NULL AFTER route_to_user_id'],
            ['name' => 'routed_at', 'sql' => 'ALTER TABLE activity_logs ADD COLUMN routed_at DATETIME NULL AFTER routed_from_user_id'],
        ];

        $anyMissing = false;
        foreach ($migrations as $migration) {
            $s = $this->pdo->query("SHOW COLUMNS FROM activity_logs LIKE '{$migration['name']}'");
            if (!($s && $s->fetch())) {
                $anyMissing = true;
                break;
            }
        }

        if (!$anyMissing) {
            $indexColumns = $this->pdo->query('SHOW INDEX FROM activity_logs')->fetchAll();
            $existingIndexes = [];
            foreach ($indexColumns as $row) {
                $existingIndexes[$row['Key_name']] = true;
            }
            $requiredIndexes = ['idx_al_work_status', 'idx_al_municipality', 'idx_al_created_at',
                'idx_al_updated_at', 'idx_al_not_archived', 'idx_al_not_archived_created',
                'idx_al_user_scope', 'idx_al_municipality_archived', 'idx_al_lo_claimant',
                'idx_al_search', 'idx_al_ft_search'];
            $anyMissing = false;
            foreach ($requiredIndexes as $idx) {
                if (!isset($existingIndexes[$idx])) {
                    $anyMissing = true;
                    break;
                }
            }
        }

        if ($anyMissing) {
            foreach ($migrations as $migration) {
                $s = $this->pdo->query("SHOW COLUMNS FROM activity_logs LIKE '{$migration['name']}'");
                if (!($s && $s->fetch())) {
                    $this->pdo->exec($migration['sql']);
                }
            }
            $this->ensureIndexes();
        }

        @touch($stampPath);
    }

    private function ensureIndexes(): void
    {
        $existing = [];
        $rows = $this->pdo->query('SHOW INDEX FROM activity_logs')->fetchAll();
        foreach ($rows as $row) {
            $existing[$row['Key_name']] = true;
        }

        $indexes = [
            'idx_al_work_status'          => 'ALTER TABLE activity_logs ADD INDEX idx_al_work_status (work_status)',
            'idx_al_municipality'         => 'ALTER TABLE activity_logs ADD INDEX idx_al_municipality (municipality)',
            'idx_al_created_at'           => 'ALTER TABLE activity_logs ADD INDEX idx_al_created_at (created_at)',
            'idx_al_updated_at'           => 'ALTER TABLE activity_logs ADD INDEX idx_al_updated_at (updated_at)',
            'idx_al_not_archived'         => 'ALTER TABLE activity_logs ADD INDEX idx_al_not_archived (archived_at, work_status)',
            'idx_al_not_archived_created' => 'ALTER TABLE activity_logs ADD INDEX idx_al_not_archived_created (archived_at, work_status, created_by)',
            'idx_al_user_scope'           => 'ALTER TABLE activity_logs ADD INDEX idx_al_user_scope (archived_at, created_by, route_to_user_id)',
            'idx_al_municipality_archived'=> 'ALTER TABLE activity_logs ADD INDEX idx_al_municipality_archived (municipality, archived_at)',
            'idx_al_lo_claimant'          => 'ALTER TABLE activity_logs ADD INDEX idx_al_lo_claimant (lo_claimant(64))',
            'idx_al_search'               => 'ALTER TABLE activity_logs ADD INDEX idx_al_search (lo_claimant(64), title_no, lot_no, received_by_control_no(64))',
            'idx_al_ft_search'            => 'ALTER TABLE activity_logs ADD FULLTEXT INDEX idx_al_ft_search (lo_claimant, title_no, lot_no, received_by_control_no)',
        ];

        foreach ($indexes as $name => $sql) {
            if (!isset($existing[$name])) {
                $this->pdo->exec($sql);
            }
        }
    }

    private function hydrate(array $row): ActivityLog
    {
        $al = new ActivityLog();
        $al->id = (int) $row['id'];
        $al->municipality = $row['municipality'];
        $al->loClaimant = $row['lo_claimant'];
        $al->titleNo = $row['title_no'];
        $al->odtsNo = $row['odts_no'] ?? null;
        $al->lotNo = $row['lot_no'] ?? null;
        $al->surveyNo = $row['survey_no'] ?? null;
        $al->areaHas = (float) ($row['area_has'] ?? 0);
        $al->location = $row['location'];
        $al->transmittedDocuments = $row['transmitted_documents'];
        $al->routeTo = $row['route_to'] ?? null;
        $al->routeToUserId = isset($row['route_to_user_id']) ? (int) $row['route_to_user_id'] : null;
        $al->routedFromUserId = isset($row['routed_from_user_id']) ? (int) $row['routed_from_user_id'] : null;
        $al->routedAt = $row['routed_at'] ?? null;
        $al->receivedByControlNo = $row['received_by_control_no'];
        $al->remarksActionTaken = $row['remarks_action_taken'];
        $al->workStatus = $row['work_status'];
        $al->archivedAt = $row['archived_at'] ?? null;
        $al->archivedBy = isset($row['archived_by']) ? (int) $row['archived_by'] : null;
        $al->createdBy = isset($row['created_by']) ? (int) $row['created_by'] : null;
        $al->updatedBy = isset($row['updated_by']) ? (int) $row['updated_by'] : null;
        $al->createdAt = $row['created_at'] ?? null;
        $al->updatedAt = $row['updated_at'] ?? null;
        $al->routeToUsername = $row['route_to_username'] ?? null;
        $al->routedFromUsername = $row['routed_from_name'] ?? null;
        $al->createdByUsername = $row['created_by_name'] ?? null;
        return $al;
    }
}

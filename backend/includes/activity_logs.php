<?php
/**
 * DAR Activity Logs - Shared activity log schema helpers
 */

function normalizeWorkStatus($status): string {
    return $status === 'finished' ? 'finished' : 'not_finished';
}

function ensureActivityLogsSchema(PDO $pdo): void {
    static $checked = false;
    if ($checked) {
        return;
    }
    $checked = true;

    $stmt = $pdo->query("SHOW COLUMNS FROM activity_logs LIKE 'work_status'");
    if (!($stmt && $stmt->fetch())) {
        $pdo->exec("ALTER TABLE activity_logs ADD COLUMN work_status ENUM('not_finished','finished') NOT NULL DEFAULT 'not_finished' AFTER remarks_action_taken");
    }

    $stmt = $pdo->query("SHOW COLUMNS FROM activity_logs LIKE 'archived_at'");
    if (!($stmt && $stmt->fetch())) {
        $pdo->exec("ALTER TABLE activity_logs ADD COLUMN archived_at DATETIME DEFAULT NULL AFTER work_status");
    }

    $stmt = $pdo->query("SHOW COLUMNS FROM activity_logs LIKE 'archived_by'");
    if (!($stmt && $stmt->fetch())) {
        $pdo->exec("ALTER TABLE activity_logs ADD COLUMN archived_by INT(11) UNSIGNED DEFAULT NULL AFTER archived_at");
        $pdo->exec("ALTER TABLE activity_logs ADD CONSTRAINT activity_logs_archived_by FOREIGN KEY (archived_by) REFERENCES users (id) ON DELETE SET NULL");
    }

    $stmt = $pdo->query("SHOW COLUMNS FROM activity_logs LIKE 'odts_no'");
    if (!($stmt && $stmt->fetch())) {
        $pdo->exec("ALTER TABLE activity_logs ADD COLUMN odts_no VARCHAR(128) DEFAULT NULL AFTER title_no");
    }

    // User routing fields (route to a specific user + history)
    $stmt = $pdo->query("SHOW COLUMNS FROM activity_logs LIKE 'route_to_user_id'");
    if (!($stmt && $stmt->fetch())) {
        $pdo->exec("ALTER TABLE activity_logs ADD COLUMN route_to_user_id INT(11) UNSIGNED DEFAULT NULL AFTER route_to");
        $pdo->exec("ALTER TABLE activity_logs ADD KEY route_to_user_id (route_to_user_id)");
        $pdo->exec("ALTER TABLE activity_logs ADD CONSTRAINT activity_logs_route_to_user_id FOREIGN KEY (route_to_user_id) REFERENCES users (id) ON DELETE SET NULL");
    }

    $stmt = $pdo->query("SHOW COLUMNS FROM activity_logs LIKE 'routed_from_user_id'");
    if (!($stmt && $stmt->fetch())) {
        $pdo->exec("ALTER TABLE activity_logs ADD COLUMN routed_from_user_id INT(11) UNSIGNED DEFAULT NULL AFTER route_to_user_id");
        $pdo->exec("ALTER TABLE activity_logs ADD KEY routed_from_user_id (routed_from_user_id)");
        $pdo->exec("ALTER TABLE activity_logs ADD CONSTRAINT activity_logs_routed_from_user_id FOREIGN KEY (routed_from_user_id) REFERENCES users (id) ON DELETE SET NULL");
    }

    $stmt = $pdo->query("SHOW COLUMNS FROM activity_logs LIKE 'routed_at'");
    if (!($stmt && $stmt->fetch())) {
        $pdo->exec("ALTER TABLE activity_logs ADD COLUMN routed_at DATETIME DEFAULT NULL AFTER routed_from_user_id");
    }

    ensureActivityLogsIndexes($pdo);
}

function ensureActivityLogsIndexes(PDO $pdo): void
{
    static $indexChecked = false;
    if ($indexChecked) return;
    $indexChecked = true;

    $existing = [];
    $rows = $pdo->query('SHOW INDEX FROM activity_logs')->fetchAll();
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
    ];

    foreach ($indexes as $name => $sql) {
        if (!isset($existing[$name])) {
            $pdo->exec($sql);
        }
    }
}

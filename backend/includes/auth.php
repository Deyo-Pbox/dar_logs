<?php
/**
 * DAR Activity Logs - Authentication & Session Helpers
 */

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

function isLoggedIn(): bool {
    return isset($_SESSION['user_id']) && !empty($_SESSION['user_id']);
}

function isAdmin(): bool {
    return isLoggedIn() && isset($_SESSION['role']) && $_SESSION['role'] === 'admin';
}

function requireLogin(): void {
    if (!isLoggedIn()) {
        $isApiRequest = false;
        if (isset($_SERVER['REQUEST_URI']) && str_contains($_SERVER['REQUEST_URI'], '/api/')) {
            $isApiRequest = true;
        }
        if (isset($_SERVER['HTTP_ACCEPT']) && str_contains($_SERVER['HTTP_ACCEPT'], 'application/json')) {
            $isApiRequest = true;
        }

        if ($isApiRequest) {
            header('Content-Type: application/json');
            http_response_code(401);
            echo json_encode(['success' => false, 'message' => 'Authentication required']);
            exit;
        }

        $base = defined('BASE_PATH') ? rtrim(BASE_PATH, '/') : '';
        header('Location: ' . ($base ? $base . '/' : '') . 'index.php');
        exit;
    }
}

function requireAdmin(): void {
    requireLogin();
    if (!isAdmin()) {
        header('HTTP/1.1 403 Forbidden');
        echo json_encode(['success' => false, 'message' => 'Admin access required']);
        exit;
    }
}

function currentUser(): ?array {
    if (!isLoggedIn()) return null;
    return [
        'id'       => (int) $_SESSION['user_id'],
        'username' => $_SESSION['username'] ?? '',
        'role'     => $_SESSION['role'] ?? 'user',
    ];
}

function updateLastActivity(): void {
    if (!isLoggedIn()) return;
    $pdo = getDB();
    ensureUsersSchema($pdo);
    $stmt = $pdo->prepare('UPDATE users SET last_activity = NOW() WHERE id = ?');
    $stmt->execute([$_SESSION['user_id']]);
}

function ensureUsersSchema(PDO $pdo): void
{
    static $checked = false;
    if ($checked) return;
    $checked = true;

    $existing = [];
    $rows = $pdo->query('SHOW INDEX FROM users')->fetchAll();
    foreach ($rows as $row) {
        $existing[$row['Key_name']] = true;
    }

    $indexes = [
        'idx_users_approved'       => 'ALTER TABLE users ADD INDEX idx_users_approved (approved)',
        'idx_users_last_activity'  => 'ALTER TABLE users ADD INDEX idx_users_last_activity (last_activity)',
        'idx_users_approved_active'=> 'ALTER TABLE users ADD INDEX idx_users_approved_active (approved, last_activity)',
    ];

    foreach ($indexes as $name => $sql) {
        if (!isset($existing[$name])) {
            $pdo->exec($sql);
        }
    }
}

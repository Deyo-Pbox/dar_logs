<?php
/**
 * DAR Activity Logs - User management (admin only)
 */

require_once __DIR__ . '/../includes/init.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $stmt = $pdo->query('SELECT id, username, role, approved, created_at, last_activity FROM users ORDER BY id');
    $rows = $stmt->fetchAll();
    echo json_encode(['success' => true, 'data' => $rows]);
    exit;
}

if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $username = trim($input['username'] ?? '');
    $password = $input['password'] ?? '';
    $role = isset($input['role']) && in_array($input['role'], ['admin', 'user'], true) ? $input['role'] : 'user';
    if (strlen($username) < 3) {
        echo json_encode(['success' => false, 'message' => 'Username must be at least 3 characters']);
        exit;
    }
    if (strlen($password) < 6) {
        echo json_encode(['success' => false, 'message' => 'Password must be at least 6 characters']);
        exit;
    }
    $stmt = $pdo->prepare('SELECT id FROM users WHERE username = ?');
    $stmt->execute([$username]);
    if ($stmt->fetch()) {
        echo json_encode(['success' => false, 'message' => 'Username already taken']);
        exit;
    }
    $hash = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
    $pdo->prepare('INSERT INTO users (username, password, role, approved) VALUES (?, ?, ?, 1)')->execute([$username, $hash, $role]);
    echo json_encode(['success' => true, 'message' => 'Account created', 'id' => (int) $pdo->lastInsertId()]);
    exit;
}

if ($method === 'PATCH') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $id = isset($input['id']) ? (int) $input['id'] : 0;
    if ($id <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid user ID']);
        exit;
    }
    if ($id === currentUser()['id']) {
        echo json_encode(['success' => false, 'message' => 'Cannot modify your own account from here']);
        exit;
    }
    $updates = [];
    $params = [];
    $username = isset($input['username']) ? trim($input['username']) : null;
    if ($username !== null) {
        if (strlen($username) < 3) {
            echo json_encode(['success' => false, 'message' => 'Username must be at least 3 characters']);
            exit;
        }
        $stmt = $pdo->prepare('SELECT id FROM users WHERE username = ? AND id != ?');
        $stmt->execute([$username, $id]);
        if ($stmt->fetch()) {
            echo json_encode(['success' => false, 'message' => 'Username already taken']);
            exit;
        }
        $updates[] = 'username = ?';
        $params[] = $username;
    }
    $password = $input['password'] ?? null;
    if ($password !== null && $password !== '') {
        if (strlen($password) < 6) {
            echo json_encode(['success' => false, 'message' => 'Password must be at least 6 characters']);
            exit;
        }
        $updates[] = 'password = ?';
        $params[] = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);
    }
    $role = $input['role'] ?? null;
    if ($role !== null && in_array($role, ['admin', 'user'], true)) {
        $updates[] = 'role = ?';
        $params[] = $role;
        if ($role === 'admin') {
            // Promoted admin accounts should be immediately usable.
            $updates[] = 'approved = 1';
        }
    }
    if (empty($updates)) {
        echo json_encode(['success' => false, 'message' => 'Nothing to update']);
        exit;
    }
    $params[] = $id;
    $pdo->prepare('UPDATE users SET ' . implode(', ', $updates) . ' WHERE id = ?')->execute($params);
    echo json_encode(['success' => true, 'message' => 'Account updated']);
    exit;
}

if ($method === 'DELETE') {
    $id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
    if ($id <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid user ID']);
        exit;
    }
    if ($id === currentUser()['id']) {
        echo json_encode(['success' => false, 'message' => 'Cannot delete your own account']);
        exit;
    }
    $pdo->prepare('DELETE FROM users WHERE id = ?')->execute([$id]);
    echo json_encode(['success' => true, 'message' => 'Account deleted']);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'message' => 'Method not allowed']);

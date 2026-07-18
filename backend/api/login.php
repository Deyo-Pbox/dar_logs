<?php
/**
 * DAR Activity Logs - Login API
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/audit.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true) ?: [];
$username = trim($input['username'] ?? '');
$password = $input['password'] ?? '';

if ($username === '' || $password === '') {
    echo json_encode(['success' => false, 'message' => 'Username and password required']);
    exit;
}

$pdo = getDB();
$stmt = $pdo->prepare('SELECT id, username, password, role, approved FROM users WHERE username = ?');
$stmt->execute([$username]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, $user['password'])) {
    echo json_encode(['success' => false, 'message' => 'Invalid username or password']);
    exit;
}

$_SESSION['user_id'] = (int) $user['id'];
$_SESSION['username'] = $user['username'];
$_SESSION['role'] = $user['role'];

$pdo->prepare('UPDATE users SET last_activity = NOW() WHERE id = ?')->execute([$user['id']]);
$pdo->prepare('INSERT INTO audit_log (user_id, username, action, table_name) VALUES (?, ?, ?, ?)')->execute([$user['id'], $user['username'], 'login', null]);
pruneAuditLogTable($pdo);

echo json_encode([
    'success' => true,
    'user' => [
        'id' => (int) $user['id'],
        'username' => $user['username'],
        'role' => $user['role'],
    ],
]);

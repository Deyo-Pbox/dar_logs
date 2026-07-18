<?php
/**
 * DAR Activity Logs - Registration API
 */

require_once __DIR__ . '/../includes/init.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true) ?: [];
$username = trim($input['username'] ?? '');
$password = $input['password'] ?? '';

if (strlen($username) < 3) {
    echo json_encode(['success' => false, 'message' => 'Username must be at least 3 characters']);
    exit;
}
if (strlen($password) < 6) {
    echo json_encode(['success' => false, 'message' => 'Password must be at least 6 characters']);
    exit;
}

$pdo = getDB();
$stmt = $pdo->prepare('SELECT id FROM users WHERE username = ?');
$stmt->execute([$username]);
if ($stmt->fetch()) {
    echo json_encode(['success' => false, 'message' => 'Username already taken']);
    exit;
}

$hash = password_hash($password, PASSWORD_DEFAULT);
$stmt = $pdo->prepare('INSERT INTO users (username, password, role, approved) VALUES (?, ?, ?, ?)');
$stmt->execute([$username, $hash, 'user', 0]);

echo json_encode(['success' => true, 'message' => 'Registration successful. Account pending admin approval.']);

<?php
require_once __DIR__ . '/../includes/init.php';
header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: [];
    $action = $input['action'] ?? 'ping';

    if ($action === 'ping') {
        echo json_encode(['success' => true, 'message' => 'Mobile sync endpoint ready']);
        exit;
    }

    if ($action === 'sync_record') {
        $username = trim((string) ($input['username'] ?? ''));
        $password = (string) ($input['password'] ?? '');
        if ($username === '' || $password === '') {
            echo json_encode(['success' => false, 'message' => 'Username/password required']);
            exit;
        }

        $stmt = $pdo->prepare('SELECT id FROM users WHERE username = ?');
        $stmt->execute([$username]);
        if ($stmt->fetch()) {
            echo json_encode(['success' => true, 'message' => 'Record queued for sync']);
            exit;
        }

        echo json_encode(['success' => false, 'message' => 'User not found']);
        exit;
    }
}

echo json_encode(['success' => false, 'message' => 'Method not allowed']);

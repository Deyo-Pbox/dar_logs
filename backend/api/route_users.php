<?php
/**
 * DAR Activity Logs - Route-to user list (for dropdowns, cached 2 min)
 */

require_once __DIR__ . '/../includes/init.php';
require_once __DIR__ . '/../includes/cache.php';

header('Content-Type: application/json');
requireLogin();

$pdo = getDB();
$user = currentUser();
$userId = (int) $user['id'];

$cacheKey = 'route_users_' . $userId;
$cached = darCacheGet($cacheKey);
if ($cached !== null) {
    echo json_encode(['success' => true, 'data' => $cached]);
    exit;
}

$stmt = $pdo->prepare("SELECT id, username, role FROM users WHERE approved = 1 AND id <> ? ORDER BY username");
$stmt->execute([$userId]);
$rows = $stmt->fetchAll();

if (empty($rows)) {
    $stmt = $pdo->prepare("SELECT id, username, role FROM users WHERE id <> ? ORDER BY username");
    $stmt->execute([$userId]);
    $rows = $stmt->fetchAll();
}

darCacheSet($cacheKey, $rows, 120);
echo json_encode(['success' => true, 'data' => $rows]);

<?php

declare(strict_types=1);

use DarLogs\Core\Router;
use DarLogs\Controllers\AuthController;
use DarLogs\Controllers\ActivityController;
use DarLogs\Controllers\UserController;
use DarLogs\Controllers\NotificationController;
use DarLogs\Controllers\DashboardController;
use DarLogs\Middleware\AuthMiddleware;
use DarLogs\Middleware\AdminMiddleware;
use DarLogs\Middleware\CorsMiddleware;
use DarLogs\Middleware\RateLimitMiddleware;

return function (Router $router): void {
    $router->addGlobalMiddleware(new CorsMiddleware());

    // Public — rate-limited
    $router->post('/api/v1/auth/login',    [AuthController::class, 'login'],    [new RateLimitMiddleware(10, 60)]);
    $router->post('/api/v1/auth/register', [AuthController::class, 'register']);

    // Activities — literal routes before parameterized routes
    $router->get('/api/v1/activities',              [ActivityController::class, 'index'],     [new AuthMiddleware()]);
    $router->get('/api/v1/activities/archived',     [ActivityController::class, 'archived'],  [new AuthMiddleware(), new AdminMiddleware()]);
    $router->get('/api/v1/activities/{id}',         [ActivityController::class, 'show'],      [new AuthMiddleware()]);
    $router->post('/api/v1/activities',             [ActivityController::class, 'store'],     [new AuthMiddleware()]);
    $router->put('/api/v1/activities/{id}',         [ActivityController::class, 'update'],    [new AuthMiddleware()]);
    $router->delete('/api/v1/activities/{id}',       [ActivityController::class, 'archive'],   [new AuthMiddleware()]);
    $router->post('/api/v1/activities/{id}/route',   [ActivityController::class, 'route'],     [new AuthMiddleware()]);
    $router->post('/api/v1/activities/{id}/restore', [ActivityController::class, 'restore'],   [new AuthMiddleware(), new AdminMiddleware()]);
    $router->delete('/api/v1/activities/{id}/permanent', [ActivityController::class, 'destroy'], [new AuthMiddleware(), new AdminMiddleware()]);
    $router->get('/api/v1/audit',                   [ActivityController::class, 'audit'],     [new AuthMiddleware()]);

    // Users — literal routes before parameterized routes
    $router->get('/api/v1/users',            [UserController::class, 'index'],    [new AuthMiddleware(), new AdminMiddleware()]);
    $router->get('/api/v1/users/approved',    [UserController::class, 'approved'], [new AuthMiddleware()]);
    $router->post('/api/v1/users',           [UserController::class, 'store'],    [new AuthMiddleware(), new AdminMiddleware()]);
    $router->get('/api/v1/users/{id}',        [UserController::class, 'show'],     [new AuthMiddleware(), new AdminMiddleware()]);
    $router->put('/api/v1/users/{id}',        [UserController::class, 'update'],   [new AuthMiddleware(), new AdminMiddleware()]);
    $router->delete('/api/v1/users/{id}',     [UserController::class, 'destroy'],  [new AuthMiddleware(), new AdminMiddleware()]);

    // Notifications
    $router->get('/api/v1/notifications',              [NotificationController::class, 'index'],       [new AuthMiddleware()]);
    $router->get('/api/v1/notifications/count',        [NotificationController::class, 'count'],       [new AuthMiddleware()]);
    $router->patch('/api/v1/notifications/{id}/read',   [NotificationController::class, 'markRead'],    [new AuthMiddleware()]);
    $router->post('/api/v1/notifications/read-all',    [NotificationController::class, 'markAllRead'], [new AuthMiddleware()]);
    $router->get('/api/v1/notifications/stream',       [NotificationController::class, 'stream'],      [new AuthMiddleware()]);

    // Dashboard
    $router->get('/api/v1/dashboard/stats',          [DashboardController::class, 'stats'],         [new AuthMiddleware()]);
    $router->get('/api/v1/dashboard/pending-count',  [DashboardController::class, 'pendingCount'],  [new AuthMiddleware()]);

    // References
    $router->get('/api/v1/references/municipalities', [DashboardController::class, 'municipalities'], [new AuthMiddleware()]);
    $router->get('/api/v1/references/users',          [DashboardController::class, 'routeUsers'],     [new AuthMiddleware()]);

    // Health check
    $router->get('/api/v1/health', [DashboardController::class, 'health']);
};

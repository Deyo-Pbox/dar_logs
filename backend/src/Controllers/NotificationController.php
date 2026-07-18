<?php

declare(strict_types=1);

namespace DarLogs\Controllers;

use DarLogs\Core\Request;
use DarLogs\Core\Response;
use DarLogs\Services\NotificationService;

class NotificationController
{
    private NotificationService $service;

    public function __construct()
    {
        $this->service = new NotificationService();
    }

    public function index(Request $request): Response
    {
        $user = $request->getUser();
        return Response::ok(['data' => $this->service->listForUser($user->id)]);
    }

    public function count(Request $request): Response
    {
        $user = $request->getUser();
        return Response::ok(['count' => $this->service->countUnread($user->id)]);
    }

    public function markRead(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $user = $request->getUser();
        $this->service->markRead($id, $user->id);
        return Response::ok(null, 'Notification marked as read');
    }

    public function markAllRead(Request $request): Response
    {
        $user = $request->getUser();
        $count = $this->service->markAllRead($user->id);
        return Response::ok(['updated' => $count], 'All notifications marked as read');
    }

    public function stream(Request $request): Response
    {
        $user = $request->getUser();
        $this->service->streamCounts($user->id);
        exit;
    }
}

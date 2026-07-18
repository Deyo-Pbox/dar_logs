<?php

declare(strict_types=1);

namespace DarLogs\Controllers;

use DarLogs\Core\Request;
use DarLogs\Core\Response;
use DarLogs\Core\Cache;
use DarLogs\Helpers\Municipalities;
use DarLogs\Services\ActivityService;
use DarLogs\Services\UserService;

class DashboardController
{
    private ActivityService $activityService;
    private UserService $userService;

    public function __construct()
    {
        $this->activityService = new ActivityService();
        $this->userService = new UserService();
    }

    public function stats(Request $request): Response
    {
        $user = $request->getUser();
        $stats = $this->activityService->getDashboardStats();
        return Response::ok([
            'stats' => $stats,
            'user'  => $user,
        ]);
    }

    public function pendingCount(Request $request): Response
    {
        $user = $request->getUser();
        $count = $this->activityService->getPendingCount($user->id);
        return Response::ok(['count' => $count]);
    }

    public function municipalities(Request $request): Response
    {
        return Response::ok([
            'municipalities' => Municipalities::list(),
            'catalog'        => Municipalities::filterCatalog(),
        ]);
    }

    public function routeUsers(Request $request): Response
    {
        return Response::ok(['data' => $this->userService->listApproved()]);
    }
}

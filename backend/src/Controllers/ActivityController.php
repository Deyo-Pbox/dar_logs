<?php

declare(strict_types=1);

namespace DarLogs\Controllers;

use DarLogs\Core\Request;
use DarLogs\Core\Response;
use DarLogs\Services\ActivityService;
use DarLogs\Validation\ActivityValidator;

class ActivityController
{
    private ActivityService $service;

    public function __construct()
    {
        $this->service = new ActivityService();
    }

    public function index(Request $request): Response
    {
        $user = $request->getUser();
        $filters = [];

        if ($request->getQuery('municipality')) {
            $filters['municipality'] = $request->getQuery('municipality');
        }
        if ($request->getQuery('work_status')) {
            $filters['work_status'] = $request->getQuery('work_status');
        }
        if ($request->getQuery('search')) {
            $filters['search'] = $request->getQuery('search');
        }

        $scope = $request->getQuery('scope');
        if ($scope !== 'all') {
            $filters['user_id'] = $user->id;
        }

        $page = max(1, (int) ($request->getQuery('page', 1)));
        $perPage = max(1, min(100, (int) ($request->getQuery('per_page', 20))));

        $result = $this->service->listActive($filters, $page, $perPage);
        return Response::ok($result);
    }

    public function show(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $record = $this->service->getById($id);
        return Response::ok($record);
    }

    public function store(Request $request): Response
    {
        $data = (new ActivityValidator())->validate($request->all());
        $id = $this->service->create($data, $request->getUser());
        return Response::created(['id' => $id], 'Record created');
    }

    public function update(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $data = (new ActivityValidator())->validate($request->all());
        $this->service->update($id, $data, $request->getUser());
        return Response::ok(null, 'Record updated');
    }

    public function archive(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $this->service->archive($id, $request->getUser());
        return Response::ok(null, 'Record archived');
    }

    public function restore(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $this->service->restore($id, $request->getUser());
        return Response::ok(null, 'Record restored');
    }

    public function destroy(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $this->service->delete($id, $request->getUser());
        return Response::ok(null, 'Record deleted');
    }

    public function route(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $targetUserId = (int) $request->getBody('target_user_id', 0);
        $this->service->route($id, $targetUserId, $request->getUser());
        return Response::ok(null, 'Record routed');
    }

    public function archived(Request $request): Response
    {
        $page = max(1, (int) ($request->getQuery('page', 1)));
        $perPage = max(1, min(100, (int) ($request->getQuery('per_page', 20))));
        $result = $this->service->getArchived($page, $perPage);
        return Response::ok($result);
    }

    public function audit(Request $request): Response
    {
        $limit = min(200, max(1, (int) ($request->getQuery('limit', 100))));
        return Response::ok(['data' => $this->service->getAuditLog($limit)]);
    }
}

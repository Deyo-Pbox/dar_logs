<?php

declare(strict_types=1);

namespace DarLogs\Controllers;

use DarLogs\Core\Request;
use DarLogs\Core\Response;
use DarLogs\Services\UserService;
use DarLogs\Validation\UserValidator;

class UserController
{
    private UserService $service;

    public function __construct()
    {
        $this->service = new UserService();
    }

    public function index(Request $request): Response
    {
        return Response::ok(['data' => $this->service->listAll()]);
    }

    public function approved(Request $request): Response
    {
        return Response::ok(['data' => $this->service->listApproved()]);
    }

    public function show(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        return Response::ok($this->service->getById($id));
    }

    public function store(Request $request): Response
    {
        $data = (new UserValidator(false))->validate($request->all());
        $result = $this->service->create($data['username'], $data['password'], $data['role'] ?? 'user');
        return Response::created($result, 'Account created');
    }

    public function update(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $data = (new UserValidator(true))->validate($request->all());
        $this->service->update($id, $data, $request->getUser());
        return Response::ok(null, 'Account updated');
    }

    public function destroy(Request $request): Response
    {
        $id = $request->getRouteInt('id');
        $this->service->delete($id, $request->getUser());
        return Response::ok(null, 'Account deleted');
    }
}

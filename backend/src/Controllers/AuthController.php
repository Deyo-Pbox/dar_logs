<?php

declare(strict_types=1);

namespace DarLogs\Controllers;

use DarLogs\Core\Request;
use DarLogs\Core\Response;
use DarLogs\Services\AuthService;
use DarLogs\Validation\LoginValidator;
use DarLogs\Validation\RegisterValidator;

class AuthController
{
    private AuthService $service;

    public function __construct()
    {
        $this->service = new AuthService();
    }

    public function login(Request $request): Response
    {
        $data = (new LoginValidator())->validate($request->all());
        $result = $this->service->login($data['username'], $data['password']);
        return Response::ok($result);
    }

    public function register(Request $request): Response
    {
        $data = (new RegisterValidator())->validate($request->all());
        $result = $this->service->register($data['username'], $data['password']);
        return Response::created($result, 'Registration submitted for approval');
    }
}

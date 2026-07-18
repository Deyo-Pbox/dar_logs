<?php

declare(strict_types=1);

namespace DarLogs\Services;

use DarLogs\Config\AppConfig;
use DarLogs\Core\Database;
use DarLogs\Exceptions\AuthenticationException;
use DarLogs\Exceptions\ConflictException;
use DarLogs\Exceptions\ValidationException;
use DarLogs\Repositories\AuditRepository;
use DarLogs\Repositories\Mysql\MysqlAuditRepository;
use DarLogs\Repositories\Mysql\MysqlUserRepository;
use DarLogs\Repositories\UserRepository;
use DarLogs\Security\JwtHandler;
use DarLogs\Security\PasswordHasher;
use PDO;

class AuthService
{
    private UserRepository $userRepo;
    private AuditRepository $auditRepo;

    public function __construct()
    {
        $this->userRepo = new MysqlUserRepository();
        $this->auditRepo = new MysqlAuditRepository();
        $this->userRepo->ensureSchema();
    }

    public function login(string $username, string $password): array
    {
        $user = $this->userRepo->findByUsername($username);
        if ($user === null) {
            throw new AuthenticationException('Invalid username or password');
        }

        $hash = $this->userRepo->findPasswordHash($user->id);
        if ($hash === null || !PasswordHasher::verify($password, $hash)) {
            throw new AuthenticationException('Invalid username or password');
        }

        $this->userRepo->updateLastActivity($user->id);

        $this->auditRepo->log($user->id, $user->username, 'login', 'users');
        $this->auditRepo->prune((int) AppConfig::get('AUDIT_LOG_CAPACITY', 100));

        $token = JwtHandler::issue([
            'sub'      => $user->id,
            'username' => $user->username,
            'role'     => $user->role,
        ]);

        return ['token' => $token, 'user' => $user];
    }

    public function register(string $username, string $password): array
    {
        $existing = $this->userRepo->findByUsername($username);
        if ($existing !== null) {
            throw new ConflictException('Username already taken');
        }

        $hash = PasswordHasher::hash($password);
        $id = $this->userRepo->create($username, $hash, 'user', false);

        $user = $this->userRepo->findById($id);
        return ['user' => $user];
    }
}

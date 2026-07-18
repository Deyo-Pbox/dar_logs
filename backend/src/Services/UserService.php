<?php

declare(strict_types=1);

namespace DarLogs\Services;

use DarLogs\Exceptions\ConflictException;
use DarLogs\Exceptions\NotFoundException;
use DarLogs\Core\Cache;
use DarLogs\Repositories\Mysql\MysqlUserRepository;
use DarLogs\Repositories\UserRepository;
use DarLogs\Security\PasswordHasher;

class UserService
{
    private UserRepository $userRepo;

    public function __construct()
    {
        $this->userRepo = new MysqlUserRepository();
        $this->userRepo->ensureSchema();
    }

    /** @return array */
    public function listAll(): array
    {
        return $this->userRepo->findAll();
    }

    public function listApproved(): array
    {
        return Cache::remember('approved_users', 120, fn() => $this->userRepo->findAllApproved());
    }

    public function getById(int $id): mixed
    {
        $user = $this->userRepo->findById($id);
        if ($user === null) {
            throw new NotFoundException('User not found');
        }
        return $user;
    }

    public function create(string $username, string $password, string $role = 'user'): array
    {
        $existing = $this->userRepo->findByUsername($username);
        if ($existing !== null) {
            throw new ConflictException('Username already taken');
        }

        $hash = PasswordHasher::hash($password);
        $id = $this->userRepo->create($username, $hash, $role, true);

        $user = $this->userRepo->findById($id);
        Cache::forget('approved_users');
        return ['id' => $id, 'user' => $user];
    }

    public function update(int $id, array $data, object $currentUser): void
    {
        $user = $this->userRepo->findById($id)
            ?? throw new NotFoundException('User not found');

        if ($id === $currentUser->id) {
            throw new ConflictException('Cannot modify your own account');
        }

        $fields = [];

        if (isset($data['username']) && $data['username'] !== $user->username) {
            $existing = $this->userRepo->findByUsername($data['username']);
            if ($existing !== null && $existing->id !== $id) {
                throw new ConflictException('Username already taken');
            }
            $fields['username'] = $data['username'];
        }

        if (isset($data['password']) && $data['password'] !== '') {
            $fields['password'] = PasswordHasher::hash($data['password']);
        }

        if (isset($data['role']) && $data['role'] !== $user->role) {
            $fields['role'] = $data['role'];
            if ($data['role'] === 'admin') {
                $fields['approved'] = 1;
            }
        }

        if (isset($data['approved'])) {
            $fields['approved'] = $data['approved'] ? 1 : 0;
        }

        if (!empty($fields)) {
            $this->userRepo->update($id, $fields);
            Cache::forget('approved_users');
        }
    }

    public function delete(int $id, object $currentUser): void
    {
        if ($id === $currentUser->id) {
            throw new ConflictException('Cannot delete your own account');
        }

        $user = $this->userRepo->findById($id)
            ?? throw new NotFoundException('User not found');

        $this->userRepo->delete($id);
        Cache::forget('approved_users');
    }
}

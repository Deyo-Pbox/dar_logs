<?php

declare(strict_types=1);

namespace DarLogs\Core;

class Request
{
    private array $queryParams;
    private array $body;
    private array $headers;
    private array $server;
    private ?object $user = null;
    private array $routeParams = [];

    public function __construct()
    {
        $this->queryParams = $_GET;
        $this->server      = $_SERVER;
        $this->headers     = $this->parseHeaders();

        $raw = file_get_contents('php://input');
        $decoded = json_decode($raw ?: '{}', true);
        $this->body = is_array($decoded) ? $decoded : [];
    }

    private function parseHeaders(): array
    {
        if (function_exists('getallheaders')) {
            $headers = [];
            foreach (getallheaders() as $key => $value) {
                $headers[strtoupper($key)] = $value;
            }
            return $headers;
        }

        $headers = [];
        foreach ($this->server as $key => $value) {
            if (str_starts_with($key, 'HTTP_')) {
                $name = str_replace('_', '-', substr($key, 5));
                $headers[$name] = $value;
            }
        }
        if (isset($this->server['CONTENT_TYPE'])) {
            $headers['CONTENT-TYPE'] = $this->server['CONTENT_TYPE'];
        }
        return $headers;
    }

    public function getMethod(): string
    {
        return strtoupper($this->server['REQUEST_METHOD'] ?? 'GET');
    }

    public function getPath(): string
    {
        $uri = $this->server['REQUEST_URI'] ?? '/';
        $pos = strpos($uri, '?');
        return $pos !== false ? substr($uri, 0, $pos) : $uri;
    }

    public function getHeader(string $name): ?string
    {
        return $this->headers[strtoupper($name)] ?? null;
    }

    public function getBearerToken(): ?string
    {
        $auth = $this->getHeader('AUTHORIZATION');
        if ($auth && str_starts_with($auth, 'Bearer ')) {
            return substr($auth, 7);
        }
        return null;
    }

    public function getQuery(string $key, mixed $default = null): mixed
    {
        return $this->queryParams[$key] ?? $default;
    }

    public function getBody(string $key, mixed $default = null): mixed
    {
        return $this->body[$key] ?? $default;
    }

    public function all(): array
    {
        return $this->body;
    }

    public function setUser(object $user): void
    {
        $this->user = $user;
    }

    public function getUser(): ?object
    {
        return $this->user;
    }

    public function setRouteParams(array $params): void
    {
        $this->routeParams = $params;
    }

    public function getRouteParam(string $key, mixed $default = null): mixed
    {
        return $this->routeParams[$key] ?? $default;
    }

    public function getRouteInt(string $key): int
    {
        return (int) ($this->routeParams[$key] ?? 0);
    }
}

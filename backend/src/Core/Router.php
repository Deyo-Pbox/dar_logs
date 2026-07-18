<?php

declare(strict_types=1);

namespace DarLogs\Core;

class Router
{
    private array $routes = [];
    private array $globalMiddleware = [];

    public function addGlobalMiddleware(Middleware $middleware): void
    {
        $this->globalMiddleware[] = $middleware;
    }

    public function get(string $pattern, callable|array $handler, array $middleware = []): void
    {
        $this->addRoute('GET', $pattern, $handler, $middleware);
    }

    public function post(string $pattern, callable|array $handler, array $middleware = []): void
    {
        $this->addRoute('POST', $pattern, $handler, $middleware);
    }

    public function put(string $pattern, callable|array $handler, array $middleware = []): void
    {
        $this->addRoute('PUT', $pattern, $handler, $middleware);
    }

    public function patch(string $pattern, callable|array $handler, array $middleware = []): void
    {
        $this->addRoute('PATCH', $pattern, $handler, $middleware);
    }

    public function delete(string $pattern, callable|array $handler, array $middleware = []): void
    {
        $this->addRoute('DELETE', $pattern, $handler, $middleware);
    }

    private function addRoute(string $method, string $pattern, callable|array $handler, array $middleware): void
    {
        $regex = $this->patternToRegex($pattern);
        $this->routes[] = [
            'method'     => $method,
            'pattern'    => $pattern,
            'regex'      => $regex,
            'handler'    => $handler,
            'middleware'  => $middleware,
            'paramNames' => $this->extractParamNames($pattern),
        ];
    }

    private function patternToRegex(string $pattern): string
    {
        $regex = preg_replace('/\{([a-zA-Z_]+)\}/', '(?P<$1>[^/]+)', $pattern);
        return '#^' . $regex . '$#';
    }

    private function extractParamNames(string $pattern): array
    {
        preg_match_all('/\{([a-zA-Z_]+)\}/', $pattern, $matches);
        return $matches[1] ?? [];
    }

    public function dispatch(Request $request): Response
    {
        $path   = rtrim($request->getPath(), '/') ?: '/';
        $method = $request->getMethod();

        if ($method === 'OPTIONS') {
            foreach ($this->globalMiddleware as $mw) {
                $mw->handle($request, fn($r) => Response::ok());
            }
            return Response::ok();
        }

        foreach ($this->routes as $route) {
            if ($route['method'] !== $method) {
                continue;
            }

            if (preg_match($route['regex'], $path, $matches)) {
                $params = [];
                foreach ($route['paramNames'] as $name) {
                    $params[$name] = $matches[$name] ?? null;
                }
                $request->setRouteParams($params);

                $handler = $route['handler'];

                $middlewareStack = [...$this->globalMiddleware, ...$route['middleware']];

                $core = function (Request $request) use ($handler) {
                    if (is_array($handler)) {
                        [$class, $method] = $handler;
                        $controller = new $class();
                        return $controller->$method($request);
                    }
                    return $handler($request);
                };

                while (!empty($middlewareStack)) {
                    $mw = array_pop($middlewareStack);
                    $next = $core;
                    $core = function (Request $request) use ($mw, $next) {
                        return $mw->handle($request, $next);
                    };
                }

                return $core($request);
            }
        }

        return Response::error(404, 'Route not found');
    }
}

<?php

declare(strict_types=1);

namespace DarLogs\Core;

class Response
{
    private int $statusCode = 200;
    private array $data = [];
    private string $message = '';
    private ?array $errors = null;

    public function status(int $code): self
    {
        $this->statusCode = $code;
        return $this;
    }

    public function withData(mixed $data): self
    {
        $this->data = is_array($data) ? $data : ['data' => $data];
        return $this;
    }

    public function withMessage(string $message): self
    {
        $this->message = $message;
        return $this;
    }

    public function withErrors(array $errors): self
    {
        $this->errors = $errors;
        return $this;
    }

    public function send(): never
    {
        http_response_code($this->statusCode);

        $payload = ['success' => $this->statusCode >= 200 && $this->statusCode < 300];

        if ($this->data !== []) {
            $payload += $this->data;
        }
        if ($this->message !== '') {
            $payload['message'] = $this->message;
        }
        if ($this->errors !== null) {
            $payload['errors'] = $this->errors;
        }

        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    public static function ok(mixed $data = null, string $message = ''): self
    {
        $res = new self();
        $res->status(200);
        if ($data !== null) {
            $res->withData($data);
        }
        if ($message !== '') {
            $res->withMessage($message);
        }
        return $res;
    }

    public static function created(mixed $data = null, string $message = ''): self
    {
        $res = new self();
        $res->status(201);
        if ($data !== null) {
            $res->withData($data);
        }
        if ($message !== '') {
            $res->withMessage($message);
        }
        return $res;
    }

    public static function error(int $code, string $message, ?array $errors = null): self
    {
        $res = new self();
        $res->status($code)->withMessage($message);
        if ($errors !== null) {
            $res->withErrors($errors);
        }
        return $res;
    }

    public static function sse(array $payload): void
    {
        header('Content-Type: text/event-stream');
        header('Cache-Control: no-cache');
        header('Connection: keep-alive');
        echo "event: counts\ndata: " . json_encode($payload) . "\n\n";
        ob_flush();
        flush();
    }

    public static function raw(string $body, string $contentType): void
    {
        header("Content-Type: {$contentType}");
        echo $body;
        exit;
    }
}

<?php

declare(strict_types=1);

namespace DarLogs\Security;

use DarLogs\Config\AppConfig;

class JwtHandler
{
    private const ALG = 'HS256';

    public static function issue(array $payload, ?int $expirySeconds = null): string
    {
        $now = time();
        $expiry = $expirySeconds ?? AppConfig::jwtExpiry();

        $header = ['alg' => self::ALG, 'typ' => 'JWT'];
        $claims = $payload + ['iat' => $now, 'exp' => $now + $expiry];

        $segments = [];
        $segments[] = self::base64UrlEncode(json_encode($header, JSON_UNESCAPED_SLASHES));
        $segments[] = self::base64UrlEncode(json_encode($claims, JSON_UNESCAPED_SLASHES));
        $signingInput = implode('.', $segments);
        $signature = self::sign($signingInput);
        $segments[] = self::base64UrlEncode($signature);

        return implode('.', $segments);
    }

    public static function verify(string $token): object
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            throw new \RuntimeException('Invalid token format');
        }

        [$headerB64, $payloadB64, $signatureB64] = $parts;

        $signingInput = "{$headerB64}.{$payloadB64}";
        $signature = self::base64UrlDecode($signatureB64);
        $expectedSig = self::sign($signingInput);

        if (!hash_equals($expectedSig, $signature)) {
            throw new \RuntimeException('Invalid token signature');
        }

        $payload = json_decode(self::base64UrlDecode($payloadB64), true);
        if (!is_array($payload)) {
            throw new \RuntimeException('Invalid token payload');
        }

        if (isset($payload['exp']) && (int) $payload['exp'] < time()) {
            throw new \RuntimeException('Token has expired');
        }

        return (object) $payload;
    }

    private static function sign(string $input): string
    {
        return hash_hmac('sha256', $input, AppConfig::jwtSecret(), true);
    }

    private static function base64UrlEncode(string $data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $data): string
    {
        $remainder = strlen($data) % 4;
        if ($remainder) {
            $data .= str_repeat('=', 4 - $remainder);
        }
        return base64_decode(strtr($data, '-_', '+/'), true);
    }
}

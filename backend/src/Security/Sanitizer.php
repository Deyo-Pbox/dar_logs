<?php

declare(strict_types=1);

namespace DarLogs\Security;

class Sanitizer
{
    public static function string(mixed $value, int $maxLength = 255): string
    {
        return mb_substr(htmlspecialchars(trim((string) $value), ENT_QUOTES, 'UTF-8'), 0, $maxLength);
    }

    public static function text(mixed $value): string
    {
        return htmlspecialchars(trim((string) $value), ENT_QUOTES, 'UTF-8');
    }

    public static function float(mixed $value): float
    {
        return (float) $value;
    }

    public static function int(mixed $value): int
    {
        return (int) $value;
    }
}

<?php

declare(strict_types=1);

namespace DarLogs\Helpers;

class Municipalities
{
    private const LIST = [
        'BOMBON',
        'CALABANGA',
        'CANAMAN',
        'MAGARAO',
        'NAGA',
        'OCAMPO',
        'PILI',
        'CARAMOAN',
        'GARCHITORENA',
        'GOA',
        'LAGONOY',
        'PRESENTACION',
        'SANJOSE',
        'SAGÑAY',
        'SIRUMA',
        'TIGAON',
        'TINAMBAC',
    ];

    private const LABEL_MAP = [
        'BOMBON'        => 'Bombon',
        'CALABANGA'     => 'Calabanga',
        'CANAMAN'       => 'Canaman',
        'CARAMOAN'      => 'Caramoan',
        'GARCHITORENA'  => 'Garchitorena',
        'GOA'           => 'Goa',
        'LAGONOY'       => 'Lagonoy',
        'MAGARAO'       => 'Magarao',
        'NAGA'          => 'Naga',
        'OCAMPO'        => 'Ocampo',
        'PILI'          => 'Pili',
        'PRESENTACION'  => 'Presentacion',
        'SAGÑAY'        => 'Sagñay',
        'SANJOSE'       => 'San Jose',
        'SIRUMA'        => 'Siruma',
        'TIGAON'        => 'Tigaon',
        'TINAMBAC'      => 'Tinambac',
    ];

    public static function list(): array
    {
        return self::LIST;
    }

    public static function filterCatalog(): array
    {
        return array_map(fn(string $value): array => [
            'value' => $value,
            'label' => self::LABEL_MAP[$value] ?? $value,
        ], array_values((function () {
            $sorted = self::LABEL_MAP;
            asort($sorted);
            return array_keys($sorted);
        })()));
    }

    private static function normalizeKey(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return null;
        }
        $normalized = preg_replace('/\s+/u', '', trim($value));
        if ($normalized === null || $normalized === '') {
            return null;
        }
        return function_exists('mb_strtoupper')
            ? mb_strtoupper($normalized, 'UTF-8')
            : strtoupper($normalized);
    }

    public static function validate(?string $input): ?string
    {
        $key = self::normalizeKey($input);
        if ($key === null) {
            return null;
        }

        foreach (self::LIST as $municipality) {
            if ($key === self::normalizeKey($municipality)) {
                return $municipality;
            }
        }
        return null;
    }

    public static function isValid(?string $input): bool
    {
        return self::validate($input) !== null;
    }
}

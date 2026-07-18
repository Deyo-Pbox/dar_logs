<?php
/**
 * DAR Activity Logs - Shared municipality list and validation helpers
 *
 * Delegates to DarLogs\Helpers\Municipalities when Composer autoload is available,
 * falls back to inline definitions otherwise.
 */

if (class_exists(\DarLogs\Helpers\Municipalities::class, false)) {
    function darMunicipalityList(): array {
        return \DarLogs\Helpers\Municipalities::list();
    }

    function darMunicipalityFilterCatalog(): array {
        return \DarLogs\Helpers\Municipalities::filterCatalog();
    }

    function darNormalizeMunicipalityKey(?string $value): ?string {
        return \DarLogs\Helpers\Municipalities::validate($value);
    }

    function normalizeMunicipalityInput(?string $value): ?string {
        return \DarLogs\Helpers\Municipalities::validate($value);
    }

    function renderMunicipalityOptions(?string $selected = null): void {
        $selectedKey = darNormalizeMunicipalityKey($selected);
        foreach (darMunicipalityList() as $municipality) {
            $isSelected = $selectedKey !== null && $selectedKey === darNormalizeMunicipalityKey($municipality);
            echo '<option value="' . htmlspecialchars($municipality, ENT_QUOTES, 'UTF-8') . '"' . ($isSelected ? ' selected' : '') . '>' . htmlspecialchars($municipality, ENT_QUOTES, 'UTF-8') . '</option>';
        }
    }
} else {
    // Fallback inline when Composer autoload is not available
    function darMunicipalityList(): array {
        return [
            'BOMBON', 'CALABANGA', 'CANAMAN', 'MAGARAO', 'NAGA', 'OCAMPO', 'PILI',
            'CARAMOAN', 'GARCHITORENA', 'GOA', 'LAGONOY', 'PRESENTACION', 'SANJOSE',
            'SAGÑAY', 'SIRUMA', 'TIGAON', 'TINAMBAC',
        ];
    }

    function darMunicipalityFilterCatalog(): array {
        $labels = [
            'BOMBON' => 'Bombon', 'CALABANGA' => 'Calabanga', 'CANAMAN' => 'Canaman',
            'CARAMOAN' => 'Caramoan', 'GARCHITORENA' => 'Garchitorena', 'GOA' => 'Goa',
            'LAGONOY' => 'Lagonoy', 'MAGARAO' => 'Magarao', 'NAGA' => 'Naga',
            'OCAMPO' => 'Ocampo', 'PILI' => 'Pili', 'PRESENTACION' => 'Presentacion',
            'SAGÑAY' => 'Sagñay', 'SANJOSE' => 'San Jose', 'SIRUMA' => 'Siruma',
            'TIGAON' => 'Tigaon', 'TINAMBAC' => 'Tinambac',
        ];
        asort($labels);
        return array_map(fn($v) => ['value' => $v, 'label' => $labels[$v]], array_keys($labels));
    }

    function darNormalizeMunicipalityKey(?string $value): ?string {
        if ($value === null || trim($value) === '') return null;
        $value = preg_replace('/\s+/u', '', trim($value));
        if ($value === null || $value === '') return null;
        return function_exists('mb_strtoupper') ? mb_strtoupper($value, 'UTF-8') : strtoupper($value);
    }

    function normalizeMunicipalityInput(?string $value): ?string {
        $key = darNormalizeMunicipalityKey($value);
        if ($key === null) return null;
        foreach (darMunicipalityList() as $m) {
            if ($key === darNormalizeMunicipalityKey($m)) return $m;
        }
        return null;
    }

    function renderMunicipalityOptions(?string $selected = null): void {
        $selectedKey = darNormalizeMunicipalityKey($selected);
        foreach (darMunicipalityList() as $municipality) {
            $isSelected = $selectedKey !== null && $selectedKey === darNormalizeMunicipalityKey($municipality);
            echo '<option value="' . htmlspecialchars($municipality, ENT_QUOTES, 'UTF-8') . '"' . ($isSelected ? ' selected' : '') . '>' . htmlspecialchars($municipality, ENT_QUOTES, 'UTF-8') . '</option>';
        }
    }
}

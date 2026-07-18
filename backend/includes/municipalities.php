<?php
/**
 * DAR Activity Logs - Shared municipality list and validation helpers
 */

function darMunicipalityList(): array {
    return [
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
}

/**
 * Canonical municipality codes with human-readable labels, A–Z for dashboard filter chips.
 * Values must match darMunicipalityList() (database / form values).
 */
function darMunicipalityFilterCatalog(): array {
    return [
        ['value' => 'BOMBON', 'label' => 'Bombon'],
        ['value' => 'CALABANGA', 'label' => 'Calabanga'],
        ['value' => 'CANAMAN', 'label' => 'Canaman'],
        ['value' => 'CARAMOAN', 'label' => 'Caramoan'],
        ['value' => 'GARCHITORENA', 'label' => 'Garchitorena'],
        ['value' => 'GOA', 'label' => 'Goa'],
        ['value' => 'LAGONOY', 'label' => 'Lagonoy'],
        ['value' => 'MAGARAO', 'label' => 'Magarao'],
        ['value' => 'NAGA', 'label' => 'Naga'],
        ['value' => 'OCAMPO', 'label' => 'Ocampo'],
        ['value' => 'PILI', 'label' => 'Pili'],
        ['value' => 'PRESENTACION', 'label' => 'Presentacion'],
        ['value' => 'SAGÑAY', 'label' => 'Sagñay'],
        ['value' => 'SANJOSE', 'label' => 'San Jose'],
        ['value' => 'SIRUMA', 'label' => 'Siruma'],
        ['value' => 'TIGAON', 'label' => 'Tigaon'],
        ['value' => 'TINAMBAC', 'label' => 'Tinambac'],
    ];
}

function darNormalizeMunicipalityKey(?string $value): ?string {
    if ($value === null) {
        return null;
    }

    $value = trim($value);
    if ($value === '') {
        return null;
    }

    $value = preg_replace('/\s+/u', '', $value);
    if ($value === null || $value === '') {
        return null;
    }

    return function_exists('mb_strtoupper')
        ? mb_strtoupper($value, 'UTF-8')
        : strtoupper($value);
}

function normalizeMunicipalityInput(?string $value): ?string {
    $key = darNormalizeMunicipalityKey($value);
    if ($key === null) {
        return null;
    }

    foreach (darMunicipalityList() as $municipality) {
        if ($key === darNormalizeMunicipalityKey($municipality)) {
            return $municipality;
        }
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

<?php

declare(strict_types=1);

namespace DarLogs\Validation;

use DarLogs\Exceptions\ValidationException;
use DarLogs\Helpers\Municipalities;

class ActivityValidator implements Validator
{
    public function validate(array $data): array
    {
        $errors = [];

        $municipality = Municipalities::validate($data['municipality'] ?? null);
        if ($municipality === null) {
            $errors['municipality'] = ['This is not a valid municipality. Please select from the list.'];
        }

        if (!empty($errors)) {
            throw new ValidationException('Validation failed', $errors);
        }

        return [
            'municipality'           => $municipality,
            'lo_claimant'            => trim($data['lo_claimant'] ?? ''),
            'title_no'               => trim($data['title_no'] ?? ''),
            'odts_no'                => trim($data['odts_no'] ?? '') ?: null,
            'lot_no'                 => trim($data['lot_no'] ?? ''),
            'survey_no'              => trim($data['survey_no'] ?? ''),
            'area_has'               => isset($data['area_has']) ? (float) $data['area_has'] : 0,
            'location'               => trim($data['location'] ?? ''),
            'transmitted_documents'  => trim($data['transmitted_documents'] ?? ''),
            'route_to'               => trim($data['route_to'] ?? ''),
            'route_to_user_id'       => isset($data['route_to_user_id']) ? (int) $data['route_to_user_id'] : 0,
            'received_by_control_no' => trim($data['received_by_control_no'] ?? ''),
            'remarks_action_taken'   => trim($data['remarks_action_taken'] ?? ''),
            'work_status'            => ($data['work_status'] ?? '') === 'finished' ? 'finished' : 'not_finished',
        ];
    }
}

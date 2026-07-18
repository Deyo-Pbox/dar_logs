<?php

declare(strict_types=1);

namespace DarLogs\Factory;

use DarLogs\Models\ActivityLog;

class ActivityFactory
{
    public static function fromRow(array $row): ActivityLog
    {
        $al = new ActivityLog();
        $al->id = (int) $row['id'];
        $al->municipality = $row['municipality'];
        $al->loClaimant = $row['lo_claimant'];
        $al->titleNo = $row['title_no'];
        $al->odtsNo = $row['odts_no'] ?? null;
        $al->lotNo = $row['lot_no'] ?? null;
        $al->surveyNo = $row['survey_no'] ?? null;
        $al->areaHas = (float) ($row['area_has'] ?? 0);
        $al->location = $row['location'];
        $al->transmittedDocuments = $row['transmitted_documents'];
        $al->routeTo = $row['route_to'] ?? null;
        $al->routeToUserId = isset($row['route_to_user_id']) ? (int) $row['route_to_user_id'] : null;
        $al->routedFromUserId = isset($row['routed_from_user_id']) ? (int) $row['routed_from_user_id'] : null;
        $al->routedAt = $row['routed_at'] ?? null;
        $al->receivedByControlNo = $row['received_by_control_no'];
        $al->remarksActionTaken = $row['remarks_action_taken'];
        $al->workStatus = $row['work_status'];
        $al->archivedAt = $row['archived_at'] ?? null;
        $al->archivedBy = isset($row['archived_by']) ? (int) $row['archived_by'] : null;
        $al->createdBy = isset($row['created_by']) ? (int) $row['created_by'] : null;
        $al->updatedBy = isset($row['updated_by']) ? (int) $row['updated_by'] : null;
        $al->createdAt = $row['created_at'] ?? null;
        $al->updatedAt = $row['updated_at'] ?? null;
        $al->routeToUsername = $row['route_to_username'] ?? null;
        $al->routedFromUsername = $row['routed_from_name'] ?? null;
        $al->createdByUsername = $row['created_by_name'] ?? null;
        return $al;
    }
}

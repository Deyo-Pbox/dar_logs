<?php

declare(strict_types=1);

namespace DarLogs\Models;

use JsonSerializable;

class ActivityLog implements JsonSerializable
{
    public int $id;
    public string $municipality;
    public string $loClaimant;
    public string $titleNo;
    public ?string $odtsNo;
    public ?string $lotNo;
    public ?string $surveyNo;
    public float $areaHas;
    public string $location;
    public string $transmittedDocuments;
    public ?string $routeTo;
    public ?int $routeToUserId;
    public ?int $routedFromUserId;
    public ?string $routedAt;
    public string $receivedByControlNo;
    public string $remarksActionTaken;
    public string $workStatus;
    public ?string $archivedAt;
    public ?int $archivedBy;
    public ?int $createdBy;
    public ?int $updatedBy;
    public ?string $createdAt;
    public ?string $updatedAt;
    public ?string $routeToUsername;
    public ?string $routedFromUsername;
    public ?string $createdByUsername;

    public function jsonSerialize(): array
    {
        return [
            'id'                     => $this->id,
            'municipality'           => $this->municipality,
            'lo_claimant'            => $this->loClaimant,
            'title_no'               => $this->titleNo,
            'odts_no'                => $this->odtsNo,
            'lot_no'                 => $this->lotNo,
            'survey_no'              => $this->surveyNo,
            'area_has'               => $this->areaHas,
            'location'               => $this->location,
            'transmitted_documents'  => $this->transmittedDocuments,
            'route_to'               => $this->routeTo,
            'route_to_user_id'       => $this->routeToUserId,
            'routed_from_user_id'    => $this->routedFromUserId,
            'routed_at'              => $this->routedAt,
            'received_by_control_no' => $this->receivedByControlNo,
            'remarks_action_taken'   => $this->remarksActionTaken,
            'work_status'            => $this->workStatus,
            'archived_at'            => $this->archivedAt,
            'archived_by'            => $this->archivedBy,
            'created_by'             => $this->createdBy,
            'updated_by'             => $this->updatedBy,
            'created_at'             => $this->createdAt,
            'updated_at'             => $this->updatedAt,
            'route_to_username'      => $this->routeToUsername,
            'routed_from_username'   => $this->routedFromUsername,
            'created_by_username'    => $this->createdByUsername,
        ];
    }
}

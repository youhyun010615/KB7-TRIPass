package com.tripass.checklist.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripChecklistItem {
    private Long id;
    private Long tripId;
    private Long templateId;
    private String checklistType;
    private String ddayStage;
    private String itemName;
    private Boolean isCompleted;
    private Boolean isExcluded;
    private Boolean isCustom;
    private Boolean isCarriedOver;
    private Integer isDeleted;
}
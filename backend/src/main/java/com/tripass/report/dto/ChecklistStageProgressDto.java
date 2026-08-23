package com.tripass.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistStageProgressDto {
    private String stage;
    private int completed;
    private int total;
    private String message;
}

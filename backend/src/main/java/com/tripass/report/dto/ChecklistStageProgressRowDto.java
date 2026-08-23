package com.tripass.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChecklistStageProgressRowDto {
    private String stage;
    private int completed;
    private int total;
}

package com.tripass.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChecklistProgressRowDto {
    private Integer completed;
    private Integer total;
}

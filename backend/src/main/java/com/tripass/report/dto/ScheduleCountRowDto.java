package com.tripass.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleCountRowDto {
    private Integer total;
    private Integer prepaid;
    private Integer onsite;
}

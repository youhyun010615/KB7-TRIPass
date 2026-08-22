package com.tripass.schedule.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@ApiModel(description = "여행 일정 등록 응답")
public class ScheduleCreateResponseDto {

    @ApiModelProperty(
            value = "등록된 여행 일정 ID",
            example = "10"
    )
    private Long scheduleId;
}
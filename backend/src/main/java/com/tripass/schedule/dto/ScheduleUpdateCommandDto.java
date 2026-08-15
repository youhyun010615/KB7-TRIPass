package com.tripass.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Builder
public class ScheduleUpdateCommandDto {

    private Long tripId;
    private Long scheduleId;
    private Long tripCountryId;
    private Long currencyId;
    private String scheduleName;
    private Timestamp scheduledAt;
    private BigDecimal amount;
    private String paymentStatus;
    private String scheduleStatus;
    private String placeName;
    private String placeAddress;
    private String memo;
}
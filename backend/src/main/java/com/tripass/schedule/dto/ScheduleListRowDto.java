package com.tripass.schedule.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleListRowDto {

    private Long id;
    private Long tripId;
    private Long tripCountryId;
    private String countryName;
    private String timeZone;
    private String scheduleName;
    private Timestamp startAt;
    private Timestamp endAt;
    private BigDecimal amount;
    private String currencyCode;
    private String currencySymbol;
    private String paymentStatus;
    private String scheduleStatus;
    private String placeName;
    private String placeAddress;
}
package com.tripass.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleListResponseDto {

    private Long id;
    private Long tripId;
    private Long tripCountryId;

    private String countryName;

    private String scheduleName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endAt;
    private String timeZone;

    private BigDecimal amount;
    private String currencyCode;
    private String currencySymbol;

    private String paymentStatus;
    private String scheduleStatus;

    private String placeName;
    private String placeAddress;
}

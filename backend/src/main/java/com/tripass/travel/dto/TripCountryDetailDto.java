package com.tripass.travel.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 현재 여행 조회 응답에 포함되는 국가별 상세 정보입니다. */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCountryDetailDto {

    private Long tripCountryId;
    private String countryName;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private BigDecimal targetBudget;
}

package com.tripass.travel.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 여행에 포함된 국가별 방문 기간과 목표 예산을 나타내는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCountry {

    private Long id;
    private Long tripId;
    private Long countryId;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private BigDecimal targetBudget;
    private Integer displayOrder;
}

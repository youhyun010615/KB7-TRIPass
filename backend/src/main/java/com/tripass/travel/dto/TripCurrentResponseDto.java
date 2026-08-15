package com.tripass.travel.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 로그인 사용자의 가장 최근 여행 정보를 반환하는 응답 DTO입니다. 등록된 여행이 없으면 null입니다. */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCurrentResponseDto {

    private Long tripId;
    private String tripName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalTargetAmount;
    private List<TripCountryDetailDto> countries;
}

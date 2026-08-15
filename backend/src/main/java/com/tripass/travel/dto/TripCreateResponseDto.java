package com.tripass.travel.dto;

import lombok.*;

import java.util.List;

/** 여행 등록 결과로 생성된 여행 ID와 국가별 요약 정보를 반환하는 응답 DTO입니다. */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateResponseDto {

    private Long tripId;
    private List<TripCountrySummaryDto> countries;
}

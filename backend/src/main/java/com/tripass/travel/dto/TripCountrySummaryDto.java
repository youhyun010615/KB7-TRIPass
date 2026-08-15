package com.tripass.travel.dto;

import lombok.*;

/** 여행 등록 응답에 포함되는 국가별 요약 정보입니다. 프론트엔드가 이후 일정 등록 시 tripCountryId로 사용합니다. */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCountrySummaryDto {

    private Long tripCountryId;
    private String countryName;
}

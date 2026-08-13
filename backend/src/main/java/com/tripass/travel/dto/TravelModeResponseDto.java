package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TravelModeResponseDto {
    private Long id;                  // 여행 ID
    private Boolean isTravelMode;     // 변경된 여행 모드 상태 (true/false)
    private String currentViewMode;   // 회원 DB 반영 모드 코드 ("TRAVEL" / "SAVING")
}
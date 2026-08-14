package com.tripass.travel.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TravelModeRequestDto {
    private Boolean isTravelMode; // 필수: true (여행 모드), false (저축 모드)
}
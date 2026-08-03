package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TravelStatusResponseDto {
    private Long totalRemainingFund;
    private Long dailyAvailableAmount;
    private List<CountryStatusDto> countries;
}

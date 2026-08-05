package com.tripass.travel.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TravelStatusResponseDto {
    private Long totalRemainingFund;
    private Long dailyAvailableAmount;
    private List<CountryStatusDto> countries;
}

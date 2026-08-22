package com.tripass.travel.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TravelStatusResponseDto {
    private TripBasicInfoDto tripInfo;
    private Long totalRemainingFund;
    private List<CountryStatusDto> countries;
    private List<ScheduleDto> upcomingSchedules;
    private List<CategorySummaryDto> categorySummary;
    private Map<String, List<TravelRecentTransactionDto>> recentTransactionsByCountry;
}

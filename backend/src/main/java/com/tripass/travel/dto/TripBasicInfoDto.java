package com.tripass.travel.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripBasicInfoDto {
    private Long tripId;
    private String tripName;
    private LocalDate startDate;
    private LocalDate endDate;
}

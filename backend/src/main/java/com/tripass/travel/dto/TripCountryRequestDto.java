package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCountryRequestDto {

    @NotNull(message = "여행 국가는 필수입니다.")
    @Positive(message = "여행 국가 ID는 양수여야 합니다.")
    private Long countryId;

    @NotNull(message = "도착일은 필수입니다.")
    private LocalDate arrivalDate;

    @NotNull(message = "출발일은 필수입니다.")
    private LocalDate departureDate;

    @NotNull(message = "방문 순서는 필수입니다.")
    @Positive(message = "방문 순서는 1 이상이어야 합니다.")
    private Integer displayOrder;
}

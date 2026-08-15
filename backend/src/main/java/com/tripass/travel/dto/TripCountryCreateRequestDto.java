package com.tripass.travel.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 여행 등록 시 국가별 방문 기간과 목표 예산을 담는 요청 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripCountryCreateRequestDto {

    @NotBlank(message = "국가명은 필수입니다.")
    private String countryName;

    @NotNull(message = "도착일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "출국일은 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "목표 예산은 필수입니다.")
    @PositiveOrZero(message = "목표 예산은 0 이상이어야 합니다.")
    private BigDecimal targetBudget;
}

package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripBudgetConfirmRequestDto {
    @Valid
    @NotEmpty(message = "국가별 목표 예산은 한 개 이상 입력해야 합니다.")
    private List<CountryBudgetConfirmRequestDto> countries;
}

package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripGoalUpdateRequestDto {

    @NotBlank(message = "여행 이름은 필수입니다.")
    @Size(max = 150, message = "여행 이름은 150자 이하여야 합니다.")
    private String tripName;

    @Valid
    @NotEmpty(message = "여행 국가는 한 개 이상 선택해야 합니다.")
    private List<TripCountryRequestDto> countries;
}

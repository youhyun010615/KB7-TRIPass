package com.tripass.travel.dto;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/** 여행 등록 요청 DTO입니다. 여행 이름과 국가별 방문 기간·목표 예산 목록을 받습니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripCreateRequestDto {

    @NotBlank(message = "여행 이름은 필수입니다.")
    private String tripName;

    @NotEmpty(message = "최소 한 개 이상의 여행 국가가 필요합니다.")
    @Valid
    private List<TripCountryCreateRequestDto> countries;
}

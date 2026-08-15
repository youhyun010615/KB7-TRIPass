package com.tripass.saving.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 카테고리별 절감률 선택 저장(PUT) 요청.
 * 빈 배열은 "전체 선택 해제"를 의미하므로 @NotEmpty가 아니라 @NotNull만 건다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionSelectionRequestDto {

    @NotNull(message = "selections는 필수입니다(선택 해제는 빈 배열로 요청하세요).")
    @Size(max = 3, message = "선택은 추천 TOP 3 이내여야 합니다.")
    @Valid
    private List<@NotNull(message = "selections 안에는 null 항목을 넣을 수 없습니다.") CategorySelectionItemDto> selections;
}

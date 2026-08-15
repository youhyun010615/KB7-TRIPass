package com.tripass.saving.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * PUT 요청에 담기는 카테고리 하나의 선택 항목.
 * reductionRate가 10/30/50 중 하나인지, categoryId가 TOP 3(기타 제외)인지는 Service에서 검증한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySelectionItemDto {

    @NotNull(message = "categoryId는 필수입니다.")
    private Long categoryId;

    @NotNull(message = "reductionRate는 필수입니다.")
    private Integer reductionRate;
}

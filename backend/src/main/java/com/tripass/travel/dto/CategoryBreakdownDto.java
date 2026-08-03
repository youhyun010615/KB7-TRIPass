package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBreakdownDto {
    private String categoryName;
    private Long amount;
    private Double percentage;
}

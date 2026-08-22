package com.tripass.travel.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBreakdownDto {
    private String categoryName;
    private Long amount;
    private Double percentage;
}

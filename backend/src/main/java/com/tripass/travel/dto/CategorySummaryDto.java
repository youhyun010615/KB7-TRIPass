package com.tripass.travel.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategorySummaryDto {
    private String categoryName;
    private Long totalAmount;
    private List<CountryAmountDto> countryDetails;
}

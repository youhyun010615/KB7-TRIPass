package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryBudgetConfirmRequestDto {
    @NotNull(message = "여행 국가 ID는 필수입니다.")
    @Positive(message = "여행 국가 ID는 양수여야 합니다.")
    private Long tripCountryId;

    @NotNull @DecimalMin(value = "0", message = "예산 금액은 0 이상이어야 합니다.") @DecimalMax(value = "100000000", message = "예산 금액은 1억 원 이하여야 합니다.")
    private BigDecimal airfareAmount;
    @NotNull @DecimalMin(value = "0", message = "예산 금액은 0 이상이어야 합니다.") @DecimalMax(value = "100000000", message = "예산 금액은 1억 원 이하여야 합니다.")
    private BigDecimal lodgingAmount;
    @NotNull @DecimalMin(value = "0", message = "예산 금액은 0 이상이어야 합니다.") @DecimalMax(value = "100000000", message = "예산 금액은 1억 원 이하여야 합니다.")
    private BigDecimal activityAmount;
    @NotNull @DecimalMin(value = "0", message = "예산 금액은 0 이상이어야 합니다.") @DecimalMax(value = "100000000", message = "예산 금액은 1억 원 이하여야 합니다.")
    private BigDecimal transportAmount;
    @NotNull @DecimalMin(value = "0", message = "예산 금액은 0 이상이어야 합니다.") @DecimalMax(value = "100000000", message = "예산 금액은 1억 원 이하여야 합니다.")
    private BigDecimal foodAmount;
    @NotNull @DecimalMin(value = "0", message = "예산 금액은 0 이상이어야 합니다.") @DecimalMax(value = "100000000", message = "예산 금액은 1억 원 이하여야 합니다.")
    private BigDecimal otherAmount;
}

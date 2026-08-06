package com.tripass.schedule.dto;

import com.tripass.schedule.enums.SchedulePaymentStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "여행 일정 수정 요청")
public class ScheduleUpdateRequestDto {

    @NotNull(message = "여행 국가 ID는 필수입니다.")
    @Positive(message = "여행 국가 ID는 양수여야 합니다.")
    @ApiModelProperty(
            value = "선택한 여행 국가 ID",
            required = true,
            example = "1"
    )
    private Long tripCountryId;

    @NotBlank(message = "일정명은 필수입니다.")
    @Size(
            max = 200,
            message = "일정명은 200자 이하여야 합니다."
    )
    @ApiModelProperty(
            value = "일정명",
            required = true,
            example = "루브르 박물관 자유 관람"
    )
    private String scheduleName;

    @NotNull(message = "일정 시간은 필수입니다.")
    @ApiModelProperty(
            value = "선택한 여행 국가 기준 현지 일정 시간",
            required = true,
            example = "2026-08-28T11:00:00"
    )
    private LocalDateTime scheduledAt;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "금액은 0 이상이어야 합니다."
    )
    @Digits(
            integer = 16,
            fraction = 2,
            message = "금액은 소수점 둘째 자리까지 입력할 수 있습니다."
    )
    @ApiModelProperty(
            value = "현지 통화 기준 금액",
            example = "90.00"
    )
    private BigDecimal amount;

    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "통화 코드는 영문 대문자 3자리여야 합니다."
    )
    @ApiModelProperty(
            value = "통화 코드, 미입력 시 선택 국가의 기본 통화 사용",
            example = "EUR"
    )
    private String currencyCode;

    @NotNull(message = "결제 상태는 필수입니다.")
    @ApiModelProperty(
            value = "결제 상태",
            required = true,
            allowableValues = "PREPAID, ONSITE, UNDECIDED",
            example = "PREPAID"
    )
    private SchedulePaymentStatus paymentStatus;

    @Size(
            max = 200,
            message = "장소명은 200자 이하여야 합니다."
    )
    @ApiModelProperty(
            value = "장소명",
            example = "루브르 박물관"
    )
    private String placeName;

    @Size(
            max = 500,
            message = "주소는 500자 이하여야 합니다."
    )
    @ApiModelProperty(
            value = "장소 주소",
            example = "Rue de Rivoli, 75001 Paris, France"
    )
    private String placeAddress;

    @Size(
            max = 1000,
            message = "메모는 1000자 이하여야 합니다."
    )
    @ApiModelProperty(
            value = "일정 메모",
            example = "관람 시간을 11시로 변경"
    )
    private String memo;
}
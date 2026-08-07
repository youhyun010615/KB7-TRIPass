package com.tripass.profile.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

// 금융 프로필 수정 화면에서 신규 등록 또는 기존 급여 수정을 나타내는 DTO
@Getter
@NoArgsConstructor
public class IncomeSourceSaveRequest {
    //급여 정보 PK
    //null이면 신규 등록, 값이 있으면 급여 정보 수정
    private Long id;
    //급여 입금 계좌 pk
    @NotNull(message = "급여 입금 계좌를 선택해 주세요.")
    private Long accountId;

    //급여 정보 구분을 위한 이름
    @NotBlank(message = "급여명을 입력해 주세요.")
    @Size(
            max = 150,
            message = "급여명은 150자 이내로 입역해주세요"
    )
    private String paymentName;

    //매월 급여 입금 예정일
    @NotNull(message = "급여일을 입력해 주세요.")
    @Min(
            value = 1,
            message = "급여일은 1일 이상이어야 합니다."
    )
    @Max(
            value = 31,
            message = "급여일은 31일 이하여야 합니다."
    )
    private Integer paymentDay;

    //월 급여액
    @NotNull(message = "월 급여액을 입력해 주세요.")
    @DecimalMin(
            value = "1",
            message = "월 급여액은 1원 이상이어야 합니다."
    )
    @Digits(
            integer = 16,
            fraction = 0,
            message = "월 급여액은 원 단위 정수로 입력해 주세요."
    )
    private BigDecimal grossAmount;

    //급여 관련 사용자 메모
    @Size(
            max = 500,
            message = "메모는 500자 이내로 입력해 주세요."
    )
    private String memo;
}

package com.tripass.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

//급여 정보 조회 응답 DTO
@Getter
@AllArgsConstructor
public class IncomeSourceResponse {

    //급여 정보 PK
    private Long id;

    //급여 입금 계좌 PK
    private Long accountId;

    // 급여 입금 계좌명
    private String accountName;

    // 화면에 표시할 계좌번호
    private String accountNumber;

    // 급여명
    private String paymentName;

    // 매월 급여일
    private Integer paymentDay;

    // 월 급여액
    private BigDecimal grossAmount;

    // 급여 관련 메모
    private String memo;
}

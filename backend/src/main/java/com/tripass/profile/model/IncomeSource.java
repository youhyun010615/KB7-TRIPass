package com.tripass.profile.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

//income_sources 테이블의 급여 정보 모델
@Getter
@Setter
@NoArgsConstructor
public class IncomeSource {

    // 급여 정보 PK
    private Long id;

    // 급여 정보를 등록한 회원 PK
    private Long userId;

    // 급여가 입금되는 계좌 PK
    private Long accountId;

    // 조회 시 함께 반환할 계좌명
    private String accountName;

    // 조회 시 함께 반환할 계좌번호
    private String accountNumber;

    // 급여명
    private String paymentName;

    // 매월 급여일(1~31)
    private Integer paymentDay;

    // 급여 관련 메모
    private String memo;

    // 월 급여액
    private BigDecimal grossAmount;

    // 논리 삭제 여부
    private boolean deleted;

    // 논리 삭제일시
    private Date deletedAt;

    // 생성일시
    private Date createdAt;

    // 수정일시
    private Date updatedAt;
}

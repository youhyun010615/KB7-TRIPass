/**
 * 개별 계좌 거래내역 조회 응답 DTO
 * 계좌 잔액과 거래내역 리스트를 함께 반환
 */

package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AccountTransactionResponseDto {
    private String accountName;   // 계좌명
    private String accountNumber; // 계좌번호
    private String accountType;   // 계좌 유형
    private BigDecimal balance; //계좌 잔액
    private List<TransactionDto> transactions; // 거래내역 리스트
}

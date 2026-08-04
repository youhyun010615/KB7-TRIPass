/**
 * 거래내역 조회 요청 파라미터를 담는 DTO
 * 프론트에서 accountId, 조회 기간(startDate, endDate)을 받아 Codef API 호출에 사용
 */

package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TransactionRequestDto {
    private Long accountId;
    private LocalDate startDate; // "2024-01-01" (yyyy-MM-dd)
    private LocalDate endDate;   // "2024-12-31" (yyyy-MM-dd)
}

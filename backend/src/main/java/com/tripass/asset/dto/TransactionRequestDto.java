/**
 * 거래내역 조회 요청 파라미터를 담는 DTO
 * 프론트에서 accountId, 조회 기간(startDate, endDate)을 받아 Codef API 호출에 사용
 */

package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionRequestDto {
    private Long accountId;
    private String organizationCode;
    private String startDate; //"20240101"
    private String endDate; //"20241231"
}

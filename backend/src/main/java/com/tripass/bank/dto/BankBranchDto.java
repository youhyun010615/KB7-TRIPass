package com.tripass.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankBranchDto {
    private Long id;
    private String bankName;
    private String branchName;
    private String address;
    private String businessHours;
    private String telephone; // API 명세의 phoneNumber -> telephone 매핑
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distance; // 반경 검색 시 사용
}

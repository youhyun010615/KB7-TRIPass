package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 월 목표 자동 송금 설정 내용을 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletAutoSavingResponseDto {

    private Long id;
    private Long sourceAccountId;
    private String sourceAccountName;
    private BigDecimal amount;
    private Integer dayOfMonth;
    private Boolean enabled;
    private LocalDate nextTransferDate;
}

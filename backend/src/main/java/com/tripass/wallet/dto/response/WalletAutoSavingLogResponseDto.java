package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 월렛 알림 화면에 표시할 자동 채우기 성공/실패 기록 정보를 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletAutoSavingLogResponseDto {

    private Long id;
    private String status;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime executedAt;
}

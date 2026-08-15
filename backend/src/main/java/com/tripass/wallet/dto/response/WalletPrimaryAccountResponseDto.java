package com.tripass.wallet.dto.response;

import lombok.*;

/** 월렛 메인 화면에서 표시할 대표 연동 계좌의 은행명, 계좌명, 마스킹 계좌번호를 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPrimaryAccountResponseDto {
    private Long accountId;
    private String bankName;
    private String accountName;
    private String maskedAccountNumber;
}
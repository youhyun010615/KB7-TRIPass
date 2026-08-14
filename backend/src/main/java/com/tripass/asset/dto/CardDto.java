package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 회원이 연동한 카드 정보입니다.
 *
 * 실제 카드번호는 API에 노출하지 않고, 원천 API에서 받은 마스킹 값만 반환합니다.
 */
@Getter
@Setter
public class CardDto {

    private Long id;
    private String cardName;
    private String cardCompany;
    private String maskedCardNumber;
    private String connectionType;
    private LocalDateTime lastSyncedAt;
}

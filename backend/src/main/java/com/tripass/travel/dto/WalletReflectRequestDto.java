package com.tripass.travel.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** 여행 저축 집계 시작 시점의 월렛 잔액을 여행 목표에 반영할지 선택하는 요청 DTO입니다. */
@Getter
@NoArgsConstructor
public class WalletReflectRequestDto {
    private boolean reflect;
    private Long targetAccountId;
}

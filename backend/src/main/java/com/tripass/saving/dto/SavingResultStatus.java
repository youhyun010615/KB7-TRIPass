package com.tripass.saving.dto;

/**
 * 저축 결과 집계 가능 여부.
 *
 * 실제 저축 금액을 산출할 데이터(TRIP 월렛 거래 원장 등)가 아직 없으므로,
 * 0원으로 계산하는 대신 UNAVAILABLE로 명시해 "저축 실패"와 "집계 불가"를 구분한다.
 */
public enum SavingResultStatus {
    AVAILABLE,
    UNAVAILABLE
}

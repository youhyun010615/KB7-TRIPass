package com.tripass.checklist.domain;

import com.tripass.checklist.exception.ChecklistErrorCode;
import com.tripass.checklist.exception.ChecklistException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DDayStage {
    D30("D30"),
    D7("D7"),
    D1("D1");

    private final String value;

    // 문자열을 Enum으로 안전하게 변환 및 유효성 검증
    public static DDayStage from(String input) {
        if (input == null || input.isBlank()) {
            throw new ChecklistException(ChecklistErrorCode.INVALID_INPUT_VALUE, "D-Day 단계(ddayStage)는 필수입니다.");
        }
        return Arrays.stream(values())
                .filter(stage -> stage.getValue().equalsIgnoreCase(input.trim()))
                .findFirst()
                .orElseThrow(() -> new ChecklistException(
                        ChecklistErrorCode.INVALID_INPUT_VALUE,
                        "유효하지 않은 D-Day 단계입니다. (허용값: D30, D7, D1)"
                ));
    }
}
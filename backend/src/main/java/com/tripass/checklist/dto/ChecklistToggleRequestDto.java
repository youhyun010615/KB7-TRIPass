package com.tripass.checklist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChecklistToggleRequestDto {
    private Boolean isCompleted; // 완료 여부 (true / false)
}
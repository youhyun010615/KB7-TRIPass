package com.tripass.ocr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 저장된 영수증 공동결제 참여자 응답 DTO
@Getter
@AllArgsConstructor
public class ReceiptParticipantResponse {

    // 공동결제 참여자 PK
    private Long id;

    // 참여자 이름
    private String participantName;

    // 표시 순서
    private Integer displayOrder;
}
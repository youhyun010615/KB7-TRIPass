package com.tripass.ocr.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 영수증 공동결제 참여자 모델
@Getter
@Setter
@NoArgsConstructor
public class ReceiptParticipant {

    // 공동결제 참여자 PK
    private Long id;

    // 참여자가 등록된 영수증 PK
    private Long receiptId;

    // 로그인 회원을 제외한 공동결제 참여자 이름
    private String participantName;

    // 참여자 표시 순서
    private Integer displayOrder;

    // 논리 삭제 여부
    private Boolean isDeleted;

    // 논리 삭제일시
    private LocalDateTime deletedAt;

    // 생성일시
    private LocalDateTime createdAt;

    // 수정일시
    private LocalDateTime updatedAt;
}
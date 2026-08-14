package com.tripass.ocr.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

// 영수증 공동결제 참여자 저장·수정 요청 DTO
@Getter
@NoArgsConstructor
public class ReceiptParticipantSaveRequest {

    // 기존 참여자 PK
    // 신규 참여자는 null로 전달한다.
    private Long id;

    // 로그인 회원을 제외한 공동결제 참여자 이름
    @NotBlank(
            message = "공동결제 참여자 이름을 입력해 주세요."
    )
    @Size(
            max = 100,
            message = "공동결제 참여자 이름은 100자 이내로 입력해 주세요."
    )
    private String participantName;
}
package com.tripass.travel.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    private Long id;                     // 여행 식별자 ID (PK)
    private Long userId;                 // 회원 ID (FK)
    private String tripName;             // 여행 이름
    private String status;               // 여행 상태 (PLANNING / TRAVELING / ENDED)
    private LocalDate startDate;         // 여행 시작일
    private LocalDate endDate;           // 여행 종료일
    private BigDecimal totalTargetAmount;// 목표 금액
    private Boolean isDeleted;           // 삭제 여부
    private LocalDateTime deletedAt;     // 삭제 일시
    private LocalDateTime createdAt;     // 생성 일시
    private LocalDateTime updatedAt;     // 수정 일시
    private LocalDateTime startReportViewedAt; // 여행 시작 저축 리포트 팝업 확인 시각

    /**
     * 비즈니스 로직: 여행 상태(Status) 변경
     */
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }

    /**
     * 비즈니스 로직: 소프트 삭제(Soft Delete) 처리
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}

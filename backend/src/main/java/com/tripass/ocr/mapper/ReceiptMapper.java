package com.tripass.ocr.mapper;

import com.tripass.ocr.dto.internal.ReceiptDetailRow;
import com.tripass.ocr.dto.internal.ReceiptSummaryRow;
import com.tripass.ocr.model.Receipt;
import com.tripass.ocr.model.ReceiptItem;
import com.tripass.ocr.model.ReceiptParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 해외 영수증, 품목 및 공동결제 참여자 테이블에 접근하는 Mapper
@Mapper
public interface ReceiptMapper {

    // 선택한 여행이 로그인 회원의 여행인지 확인
    boolean existsTripByIdAndUserId(
            @Param("tripId") Long tripId,
            @Param("userId") Long userId
    );

    // 선택한 국가가 해당 회원의 여행에 포함된 국가인지 확인
    boolean existsTripCountryByUserId(
            @Param("tripId") Long tripId,
            @Param("countryId") Long countryId,
            @Param("userId") Long userId
    );

    // 지출 카테고리 PK 존재 여부 확인
    boolean existsCategoryById(
            @Param("categoryId") Long categoryId
    );

    // 통화 PK 존재 여부 확인
    boolean existsCurrencyById(
            @Param("currencyId") Long currencyId
    );

    // 해외 영수증 저장
    int insertReceipt(Receipt receipt);

    // 해외 영수증 품목 일괄 저장
    int insertReceiptItems(
            @Param("items") List<ReceiptItem> items
    );

    // 공동결제 참여자 일괄 저장
    int insertReceiptParticipants(
            @Param("participants")
            List<ReceiptParticipant> participants
    );

    // 로그인 회원의 특정 여행 영수증 목록 조회
    List<ReceiptSummaryRow> findAllByUserIdAndTripId(
            @Param("userId") Long userId,
            @Param("tripId") Long tripId
    );

    // 로그인 회원의 영수증 상세 조회
    ReceiptDetailRow findDetailByIdAndUserIdAndTripId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId,
            @Param("tripId") Long tripId
    );

    // 로그인 회원의 영수증 품목 조회
    List<ReceiptItem> findItemsByReceiptIdAndUserIdAndTripId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId,
            @Param("tripId") Long tripId
    );

    // 로그인 회원의 영수증 공동결제 참여자 조회
    List<ReceiptParticipant> findParticipantsByReceiptIdAndUserIdAndTripId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId,
            @Param("tripId") Long tripId
    );

    // 로그인 회원의 영수증 수정
    int updateReceipt(Receipt receipt);

    // 기존 영수증 품목 전체 논리 삭제
    int softDeleteItemsByReceiptIdAndUserIdAndTripId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId,
            @Param("tripId") Long tripId
    );

    // 기존 공동결제 참여자 전체 논리 삭제
    int softDeleteParticipantsByReceiptIdAndUserIdAndTripId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId,
            @Param("tripId") Long tripId
    );

    // 로그인 회원의 영수증 논리 삭제
    int softDeleteReceiptByIdAndUserIdAndTripId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId,
            @Param("tripId") Long tripId
    );
}
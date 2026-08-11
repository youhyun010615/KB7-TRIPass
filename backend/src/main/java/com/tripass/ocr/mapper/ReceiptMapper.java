package com.tripass.ocr.mapper;

import com.tripass.ocr.dto.internal.ReceiptDetailRow;
import com.tripass.ocr.dto.internal.ReceiptSummaryRow;
import com.tripass.ocr.model.Receipt;
import com.tripass.ocr.model.ReceiptItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 해외 영수증과 품목 테이블에 접근하는 Mapper
@Mapper
public interface ReceiptMapper {

    // 선택한 여행이 로그인 회원의 여행인지 확인
    boolean existsTripByIdAndUserId(
            @Param("tripId") Long tripId,
            @Param("userId") Long userId
    );

    // 국가 PK 존재 여부 확인
    boolean existsCountryById(
            @Param("countryId") Long countryId
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

    // 로그인 회원의 영수증 목록 조회
    List<ReceiptSummaryRow> findAllByUserId(
            @Param("userId") Long userId
    );

    // 로그인 회원의 영수증 상세 조회
    ReceiptDetailRow findDetailByIdAndUserId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId
    );

    // 로그인 회원의 영수증 품목 조회
    List<ReceiptItem> findItemsByReceiptIdAndUserId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId
    );

    // 로그인 회원의 영수증 수정
    int updateReceipt(Receipt receipt);

    // 기존 영수증 품목 전체 논리 삭제
    int softDeleteItemsByReceiptIdAndUserId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId
    );

    // 로그인 회원의 영수증 논리 삭제
    int softDeleteReceiptByIdAndUserId(
            @Param("receiptId") Long receiptId,
            @Param("userId") Long userId
    );
}
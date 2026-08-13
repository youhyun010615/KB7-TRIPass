package com.tripass.ocr.service;

import com.tripass.ocr.dto.request.ReceiptSaveRequest;
import com.tripass.ocr.dto.response.ReceiptDetailResponse;
import com.tripass.ocr.dto.response.ReceiptSummaryResponse;
import com.tripass.ocr.dto.internal.ReceiptImageData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 해외 영수증 저장·조회·수정·삭제 기능
public interface ReceiptService {

    // OCR 결과를 수정한 후 이미지와 영수증 정보를 저장한다.
    ReceiptDetailResponse createReceipt(
            Long userId,
            ReceiptSaveRequest request,
            MultipartFile receiptImage
    );

    // 로그인 회원의 특정 여행 영수증 목록을 조회한다.
    List<ReceiptSummaryResponse> getReceipts(
            Long userId,
            Long tripId
    );

    // 로그인 회원의 영수증 상세 정보를 조회한다.
    ReceiptDetailResponse getReceipt(
            Long userId,
            Long receiptId
    );

    // 저장된 영수증 정보와 품목을 수정한다.
    ReceiptDetailResponse updateReceipt(
            Long userId,
            Long receiptId,
            ReceiptSaveRequest request
    );

    // 영수증을 논리 삭제한다.
    void deleteReceipt(
            Long userId,
            Long receiptId
    );

    // 회원 소유권을 확인한 후 영수증 이미지 데이터를 반환한다.
    ReceiptImageData getReceiptImage(
            Long userId,
            Long receiptId
    );
}
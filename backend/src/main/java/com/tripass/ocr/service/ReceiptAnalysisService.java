package com.tripass.ocr.service;

import com.tripass.ocr.dto.response.ReceiptAnalyzeResponse;
import org.springframework.web.multipart.MultipartFile;

//해외 영수증 OCR 분석 기능을 정의하는 서비스 인터페이스
public interface ReceiptAnalysisService {


    ReceiptAnalyzeResponse analyzeReceipt(
            Long userId,
            MultipartFile receiptImage
    );
}
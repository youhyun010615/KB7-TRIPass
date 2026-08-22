package com.tripass.ocr.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 영수증 이미지 조회 결과
@Getter
@AllArgsConstructor
public class ReceiptImageData {

    // 원본 파일명
    private String fileName;

    // JPG 또는 PNG 파일 형식
    private String fileType;

    // 이미지 파일 데이터
    private byte[] imageBytes;
}
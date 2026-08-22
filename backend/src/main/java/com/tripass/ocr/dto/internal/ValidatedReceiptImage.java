package com.tripass.ocr.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 검증이 완료된 영수증 이미지 정보
@Getter
@AllArgsConstructor
public class ValidatedReceiptImage {

    // 사용자가 업로드한 원본 파일명
    private String originalFileName;

    // 실제 이미지 형식에 따른 확장자
    private String fileExtension;

    // 검증이 완료된 이미지 데이터
    private byte[] imageBytes;
}
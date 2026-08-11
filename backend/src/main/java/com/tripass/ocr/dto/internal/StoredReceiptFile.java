package com.tripass.ocr.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 서버 저장소에 저장된 영수증 이미지 정보
@Getter
@AllArgsConstructor
public class StoredReceiptFile {

    // 사용자가 업로드한 원본 파일명
    private String originalFileName;

    // 서버 내부에 저장된 상대 경로
    private String storedPath;

    // 실제 이미지 파일 형식
    private String fileType;
}
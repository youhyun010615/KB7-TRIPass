package com.tripass.ocr.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

//Google Cloud Vision OCR 처리 결과
@Getter
@AllArgsConstructor
public class VisionOcrResult {

    //영수증 이미지에서 인식한 전체 원문
    private String rawText;

    //Vision API가 감지한 원문 언어 코드
    private String detectedLanguageCode;

    //위치 정보가 포함된 텍스트 영역 목록
    private List<OcrTextBlock> textBlocks;
}

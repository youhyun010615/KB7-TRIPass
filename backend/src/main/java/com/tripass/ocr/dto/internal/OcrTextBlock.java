package com.tripass.ocr.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


// OCR로 인식한 텍스트 영역과 좌표 정보
@Getter
@AllArgsConstructor
public class OcrTextBlock {

    //해당 영역에서 인식한 문자열
    private String text;

    //텍스트 영역을 감싸는 꼭짓점 좌표
    private List<Vertex> vertices;

    //이미지 내 좌표
    @Getter
    @AllArgsConstructor
    public static class Vertex {

        private int x;
        private int y;
    }
}

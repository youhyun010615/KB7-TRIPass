package com.tripass.ocr.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.ocr.dto.response.ReceiptAnalyzeResponse;
import com.tripass.ocr.service.ReceiptAnalysisService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

//해외 영수증 OCR 분석 API
@Api(tags = "OCR - 해외 영수증")
@RestController
@RequestMapping("/api/v1/ocr/receipts")
public class OcrController {

    private final ReceiptAnalysisService
            receiptAnalysisService;

    public OcrController(
            ReceiptAnalysisService receiptAnalysisService
    ) {
        this.receiptAnalysisService =
                receiptAnalysisService;
    }

    /**
     * 해외 영수증 이미지를 OCR로 분석한다.
     */
    @ApiOperation(
            value = "해외 영수증 OCR 분석",
            notes = "JPG, JPEG, PNG 형식의 해외 영수증 이미지를 "
                    + "분석하고 상호명과 품목명을 한국어로 번역합니다."
    )
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<
            ApiResponse<ReceiptAnalyzeResponse>
            > analyzeReceipt(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @ApiParam(
                    value = "분석할 해외 영수증 이미지",
                    required = true
            )
            @RequestParam("file")
            MultipartFile receiptImage
    ) {
        ReceiptAnalyzeResponse response =
                receiptAnalysisService
                        .analyzeReceipt(
                                userId,
                                receiptImage
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "영수증 분석이 완료되었습니다.",
                        response
                )
        );
    }
}
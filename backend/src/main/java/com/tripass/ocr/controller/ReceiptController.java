package com.tripass.ocr.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.ocr.dto.internal.ReceiptImageData;
import com.tripass.ocr.dto.request.ReceiptSaveRequest;
import com.tripass.ocr.dto.response.ReceiptDetailResponse;
import com.tripass.ocr.dto.response.ReceiptSummaryResponse;
import com.tripass.ocr.service.ReceiptService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

// 여행별 해외 영수증 저장·조회·수정·삭제 API
@Api(tags = "OCR - 해외 영수증")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(
            ReceiptService receiptService
    ) {
        this.receiptService = receiptService;
    }

    // OCR 분석 결과를 수정한 후 선택한 여행에 영수증을 저장한다.
    @ApiOperation(
            value = "해외 영수증 저장",
            notes = "OCR 분석 결과를 사용자가 수정한 후 "
                    + "선택한 여행에 영수증 정보와 원본 이미지를 저장합니다. "
                    + "수기 입력 영수증은 이미지 없이 저장할 수 있습니다."
    )
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<
            ApiResponse<ReceiptDetailResponse>
            > createReceipt(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @ApiParam(
                    value = "영수증을 저장할 여행 PK",
                    required = true
            )
            @PathVariable("tripId")
            Long tripId,

            @ApiParam(
                    value = "저장할 영수증 정보",
                    required = true
            )
            @Valid
            @RequestPart("data")
            ReceiptSaveRequest request,

            @ApiParam(
                    value = "저장할 영수증 원본 이미지",
                    required = false
            )
            @RequestPart(
                    value = "file",
                    required = false
            )
            MultipartFile receiptImage
    ) {
        ReceiptDetailResponse response =
                receiptService.createReceipt(
                        userId,
                        tripId,
                        request,
                        receiptImage
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "영수증이 저장되었습니다.",
                                response
                        )
                );
    }

    // 로그인 회원이 소유한 특정 여행의 영수증 목록을 조회한다.
    @ApiOperation(
            value = "해외 영수증 목록 조회",
            notes = "로그인 회원이 소유한 특정 여행의 "
                    + "영수증 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<
            ApiResponse<List<ReceiptSummaryResponse>>
            > getReceipts(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @ApiParam(
                    value = "조회할 여행 PK",
                    required = true
            )
            @PathVariable("tripId")
            Long tripId
    ) {
        List<ReceiptSummaryResponse> response =
                receiptService.getReceipts(
                        userId,
                        tripId
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    // 특정 여행에 저장된 영수증 상세 정보와 품목을 조회한다.
    @ApiOperation(
            value = "해외 영수증 상세 조회",
            notes = "로그인 회원이 소유한 특정 여행의 "
                    + "영수증 상세 정보와 품목을 조회합니다."
    )
    @GetMapping("/{receiptId}")
    public ResponseEntity<
            ApiResponse<ReceiptDetailResponse>
            > getReceipt(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @ApiParam(
                    value = "여행 PK",
                    required = true
            )
            @PathVariable("tripId")
            Long tripId,

            @ApiParam(
                    value = "영수증 PK",
                    required = true
            )
            @PathVariable("receiptId")
            Long receiptId
    ) {
        ReceiptDetailResponse response =
                receiptService.getReceipt(
                        userId,
                        tripId,
                        receiptId
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    // 특정 여행에 저장된 영수증과 품목 정보를 수정한다.
    @ApiOperation(
            value = "해외 영수증 수정",
            notes = "로그인 회원이 소유한 특정 여행의 "
                    + "영수증 정보, 품목 및 공동결제 참여자를 수정합니다."
    )
    @PutMapping("/{receiptId}")
    public ResponseEntity<
            ApiResponse<ReceiptDetailResponse>
            > updateReceipt(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @ApiParam(
                    value = "여행 PK",
                    required = true
            )
            @PathVariable("tripId")
            Long tripId,

            @ApiParam(
                    value = "영수증 PK",
                    required = true
            )
            @PathVariable("receiptId")
            Long receiptId,

            @Valid
            @RequestBody
            ReceiptSaveRequest request
    ) {
        ReceiptDetailResponse response =
                receiptService.updateReceipt(
                        userId,
                        tripId,
                        receiptId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "영수증이 수정되었습니다.",
                        response
                )
        );
    }

    // 특정 여행에 저장된 영수증을 논리 삭제한다.
    @ApiOperation(
            value = "해외 영수증 삭제",
            notes = "로그인 회원이 소유한 특정 여행의 "
                    + "영수증, 품목 및 공동결제 참여자를 논리 삭제합니다."
    )
    @DeleteMapping("/{receiptId}")
    public ResponseEntity<Void> deleteReceipt(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @ApiParam(
                    value = "여행 PK",
                    required = true
            )
            @PathVariable("tripId")
            Long tripId,

            @ApiParam(
                    value = "영수증 PK",
                    required = true
            )
            @PathVariable("receiptId")
            Long receiptId
    ) {
        receiptService.deleteReceipt(
                userId,
                tripId,
                receiptId
        );

        return ResponseEntity.noContent().build();
    }

    // 회원과 여행 소유권을 확인한 후 영수증 원본 이미지를 반환한다.
    @ApiOperation(
            value = "해외 영수증 이미지 조회",
            notes = "로그인 회원이 소유한 특정 여행의 "
                    + "영수증 원본 이미지를 반환합니다."
    )
    @GetMapping("/{receiptId}/image")
    public ResponseEntity<byte[]> getReceiptImage(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @ApiParam(
                    value = "여행 PK",
                    required = true
            )
            @PathVariable("tripId")
            Long tripId,

            @ApiParam(
                    value = "영수증 PK",
                    required = true
            )
            @PathVariable("receiptId")
            Long receiptId
    ) {
        ReceiptImageData imageData =
                receiptService.getReceiptImage(
                        userId,
                        tripId,
                        receiptId
                );

        return ResponseEntity.ok()
                .contentType(
                        resolveMediaType(
                                imageData.getFileType()
                        )
                )
                .body(
                        imageData.getImageBytes()
                );
    }

    private MediaType resolveMediaType(
            String fileType
    ) {
        if ("PNG".equalsIgnoreCase(fileType)) {
            return MediaType.IMAGE_PNG;
        }

        return MediaType.IMAGE_JPEG;
    }
}
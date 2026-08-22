package com.tripass.ocr.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.ocr.dto.internal.ReceiptImageData;
import com.tripass.ocr.dto.request.ReceiptSaveRequest;
import com.tripass.ocr.dto.response.ParticipantReceiptsResponse;
import com.tripass.ocr.dto.response.ReceiptDetailResponse;
import com.tripass.ocr.dto.response.ReceiptSummaryResponse;
import com.tripass.ocr.dto.response.SettlementSummaryResponse;
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
import java.time.LocalDate;
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
                    + "영수증 목록을 조회합니다. "
                    + "startDate, endDate(yyyy-MM-dd)로 날짜 필터를 적용할 수 있습니다."
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
            Long tripId,

            @ApiParam(value = "조회 시작일 (yyyy-MM-dd)")
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @ApiParam(value = "조회 종료일 (yyyy-MM-dd)")
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        List<ReceiptSummaryResponse> response;
        if (startDate != null || endDate != null) {
            response = receiptService.getReceipts(userId, tripId, startDate, endDate);
        } else {
            response = receiptService.getReceipts(userId, tripId);
        }

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @ApiOperation(
            value = "공동결제 참여자별 정산 요약",
            notes = "여행의 모든 공동결제 참여자별 받아야 할 금액을 조회합니다."
    )
    @GetMapping("/settlements")
    public ResponseEntity<
            ApiResponse<SettlementSummaryResponse>
            > getSettlementSummary(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @PathVariable("tripId")
            Long tripId
    ) {
        SettlementSummaryResponse response =
                receiptService.getSettlementSummary(userId, tripId);

        return ResponseEntity.ok(
                ApiResponse.success("정산 요약 조회 성공", response)
        );
    }

    @ApiOperation(
            value = "특정 참여자 정산 상세",
            notes = "특정 참여자가 포함된 영수증 목록과 총 정산 금액을 조회합니다."
    )
    @GetMapping("/settlements/{participantName}")
    public ResponseEntity<
            ApiResponse<ParticipantReceiptsResponse>
            > getParticipantReceipts(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @PathVariable("tripId")
            Long tripId,

            @PathVariable("participantName")
            String participantName
    ) {
        ParticipantReceiptsResponse response =
                receiptService.getParticipantReceipts(userId, tripId, participantName);

        return ResponseEntity.ok(
                ApiResponse.success("참여자 정산 상세 조회 성공", response)
        );
    }

    @ApiOperation(
            value = "참여자 정산 완료 토글",
            notes = "특정 참여자의 정산 상태를 완료/미완료로 변경합니다."
    )
    @PutMapping("/settlements/{participantName}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleSettlement(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @PathVariable("tripId")
            Long tripId,

            @PathVariable("participantName")
            String participantName,

            @org.springframework.web.bind.annotation.RequestParam
            boolean settled
    ) {
        receiptService.toggleParticipantSettlement(userId, tripId, participantName, settled);

        return ResponseEntity.ok(
                ApiResponse.success(
                        settled ? "정산 완료 처리되었습니다." : "정산 미완료로 변경되었습니다.",
                        null
                )
        );
    }

    @ApiOperation(
            value = "영수증 등록 날짜 목록",
            notes = "영수증이 등록된 날짜 목록을 반환합니다."
    )
    @GetMapping("/dates")
    public ResponseEntity<ApiResponse<List<String>>> getReceiptDates(
            @ApiIgnore
            @AuthenticationPrincipal
            Long userId,

            @PathVariable("tripId")
            Long tripId
    ) {
        List<String> dates = receiptService.getReceiptDates(userId, tripId);

        return ResponseEntity.ok(
                ApiResponse.success("영수증 날짜 목록 조회 성공", dates)
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
package com.tripass.asset.controller;

import com.tripass.asset.dto.CalendarDayDto;
import com.tripass.asset.dto.TransactionDto;
import com.tripass.asset.dto.TransactionReclassificationResponseDto;
import com.tripass.asset.dto.TransactionUpdateRequestDto;
import com.tripass.asset.service.AssetService;
import com.tripass.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final AssetService assetService;

    public TransactionController(AssetService assetService) {
        this.assetService = assetService;
    }

    //AST-005: 전체 계좌 거래내역 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getAllTransactions(
            Authentication authentication,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.getAllTransactions(userId, startDate, endDate)));
    }

    //AST-006: 거래내역 단건 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransactionDetail(
            Authentication authentication,
            @PathVariable("id") Long transactionId) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.getTransactionDetail(userId, transactionId)));
    }

    //거래내역 수정
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateTransaction(
            Authentication authentication,
            @PathVariable("id") Long transactionId,
            @RequestBody TransactionUpdateRequestDto req) {
        Long userId = getAuthenticatedUserId(authentication);
        assetService.updateTransaction(userId, transactionId, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/classification/reclassify")
    public ResponseEntity<ApiResponse<TransactionReclassificationResponseDto>> reclassifyTransactions(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        TransactionReclassificationResponseDto result =
                assetService.reclassifyCardTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    //거래내역 캘린더 조회
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<CalendarDayDto>>> getCalendar(
            Authentication authentication,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String type) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.getCalendar(userId, year, month, type)));
    }
}

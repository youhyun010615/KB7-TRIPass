package com.tripass.asset.controller;

import com.tripass.asset.dto.*;
import com.tripass.asset.service.AssetService;
import com.tripass.common.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    //AST-001: 자산(계좌) 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAccounts(
            Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.getAccounts(userId)));
    }

    //AST-002 계좌연동
    @PostMapping("/codef/connect")
    public ResponseEntity<ApiResponse<List<AccountDto>>> linkBank(
            Authentication authentication,
            @RequestBody CodefLinkRequestDto req) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assetService.linkBank(userId, req)));
    }

    //AST-003: 거래내역 조회 - Codef 수시입출 거래내역을 조회하고 DB에 저장 후 반환
    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> fetchTransactions(
            Authentication authentication,
            @RequestBody TransactionRequestDto req) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(assetService.fetchTransactions(userId, req)));
    }

    //지원 금융기관 목록 (프론트 은행 선택 화면용)
    @GetMapping("/institutions")
    public ResponseEntity<ApiResponse<List<SupportedInstitutionDto>>> getInstitutions() {
        return ResponseEntity.ok(ApiResponse.success(assetService.getSupportedInstitutions()));
    }

    //AST-004: 개별 계좌 거래내역 조회(DB 조회)
    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<AccountTransactionResponseDto>> getAccountTransactions(
            Authentication authentication,
            @PathVariable("id") Long accountId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String type) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                assetService.getAccountTransactions(userId, accountId, startDate, endDate, type)));
    }

    //AST-007: 계좌 연결 해제(soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            Authentication authentication,
            @PathVariable("id") Long accountId) {
        Long userId = getAuthenticatedUserId(authentication);
        assetService.deleteAccount(userId, accountId);
        return ResponseEntity.noContent().build();
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}

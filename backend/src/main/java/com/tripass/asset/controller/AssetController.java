package com.tripass.asset.controller;

import com.tripass.asset.dto.*;
import com.tripass.asset.service.AssetService;
import com.tripass.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    //AST-002 계좌연동
    @PostMapping("/link")
    public ResponseEntity<ApiResponse<List<AccountDto>>> linkBank(
            @RequestAttribute("userId") Long userId,
            @RequestBody CodefLinkRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assetService.linkBank(userId, req)));
    }

    //AST-001: 자산(계좌) 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAccounts(
            @RequestAttribute("userId") Long userId) {
        return
                ResponseEntity.ok(ApiResponse.success(assetService.getAccounts(userId)));
    }

    //지원 금융기관 목록 (프론트 은행 선택 화면용)
    @GetMapping("/institutions")
    public ResponseEntity<ApiResponse<List<SupportedInstitutionDto>>>
    getInstitutions(){
        return
                ResponseEntity.ok(ApiResponse.success(assetService.getSupportedInstitutions()));
    }
}

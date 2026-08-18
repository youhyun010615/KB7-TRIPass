package com.tripass.wallet.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.wallet.dto.request.*;
import com.tripass.wallet.dto.response.*;
import com.tripass.wallet.service.WalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

/** 월렛 조회, 충전, 출금, 계좌 연동, 자동 송금 설정 API 요청을 처리하는 컨트롤러입니다. */

@Api(tags = "월렛")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    @ApiOperation(
            value = "월렛 메인 조회",
            notes = "로그인 사용자의 월렛 잔액과 대표 연동 계좌 정보를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<WalletMainResponseDto>> getWalletMain(
            @AuthenticationPrincipal Long userId
    ) {
        WalletMainResponseDto data = walletService.getWalletMain(userId);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 메인 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "월렛 거래내역 조회",
            notes = "월렛 입금, 출금, 충전, 보정 거래내역을 조회합니다."
    )
    @GetMapping("/ledgers")
    public ResponseEntity<ApiResponse<List<WalletLedgerResponseDto>>> getWalletLedgers(
            @AuthenticationPrincipal Long userId
    ) {
        List<WalletLedgerResponseDto> data = walletService.getWalletLedgers(userId);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 거래내역 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "월별 저축 추이 상세 조회",
            notes = "월별 저축 추이 그래프에서 선택한 월의 정확한 모은 금액, 채우기/빼기 합계와 거래내역을 조회합니다."
    )
    @GetMapping("/monthly-savings/{month}")
    public ResponseEntity<ApiResponse<WalletMonthlySavingDetailResponseDto>> getMonthlySavingDetail(
            @AuthenticationPrincipal Long userId,

            @PathVariable
            String month
    ) {
        WalletMonthlySavingDetailResponseDto data = walletService.getMonthlySavingDetail(userId, month);

        return ResponseEntity.ok(
                ApiResponse.success("월별 저축 추이 상세 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "보유 외화 전체 조회",
            notes = "연동된 트래블카드의 모든 보유 외화 잔액과 원화 환산 금액을 조회합니다."
    )
    @GetMapping("/foreign-balances")
    public ResponseEntity<ApiResponse<List<WalletForeignBalanceResponseDto>>> getForeignBalances(
            @AuthenticationPrincipal Long userId
    ) {
        List<WalletForeignBalanceResponseDto> data = walletService.getAllForeignBalances(userId);

        return ResponseEntity.ok(
                ApiResponse.success("보유 외화 전체 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "월렛 수동 충전",
            notes = "연동 계좌에서 월렛으로 금액을 충전합니다."
    )
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<WalletCommandResponseDto>> charge(
            @AuthenticationPrincipal Long userId,

            @Valid
            @RequestBody
            WalletChargeRequestDto request
    ) {
        WalletCommandResponseDto data = walletService.charge(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 충전 성공", data)
        );
    }

    @ApiOperation(
            value = "월렛 출금",
            notes = "월렛 잔액을 연동 계좌로 출금합니다."
    )
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletCommandResponseDto>> withdraw(
            @AuthenticationPrincipal Long userId,

            @Valid
            @RequestBody
            WalletWithdrawRequestDto request
    ) {
        WalletCommandResponseDto data = walletService.withdraw(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 출금 성공", data)
        );
    }

    @ApiOperation(value = "월렛 출금 계좌 선택 정보 조회",
            notes = "프로필에 등록된 계좌와 최근 직접 입력한 수취계좌를 조회합니다.")
    @GetMapping("/withdraw/options")
    public ResponseEntity<ApiResponse<WalletWithdrawOptionsResponseDto>> getWithdrawOptions(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "월렛 출금 계좌 선택 정보 조회 성공",
                walletService.getWithdrawOptions(userId)
        ));
    }

    @ApiOperation(
            value = "월렛 연동 계좌 목록 조회",
            notes = "로그인 사용자의 월렛에 연결된 계좌 목록을 조회합니다."
    )
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<WalletAccountResponseDto>>> getAccounts(
            @AuthenticationPrincipal Long userId
    ) {
        List<WalletAccountResponseDto> data = walletService.getAccounts(userId);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 연동 계좌 목록 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "월렛 연결 가능 계좌 목록 조회",
            notes = "프로필에 등록된 계좌 중 아직 월렛에 연결되지 않은 계좌 목록을 조회합니다."
    )
    @GetMapping("/accounts/options")
    public ResponseEntity<ApiResponse<List<WalletAccountResponseDto>>> getAccountOptions(
            @AuthenticationPrincipal Long userId
    ) {
        List<WalletAccountResponseDto> data = walletService.getAccountOptions(userId);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 연결 가능 계좌 목록 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "월렛 계좌 연동",
            notes = "사용자가 선택한 계좌를 월렛 충전 및 출금 계좌로 연동합니다."
    )
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<Void>> linkAccount(
            @AuthenticationPrincipal Long userId,

            @Valid
            @RequestBody
            WalletAccountLinkRequestDto request
    ) {
        walletService.linkAccount(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 계좌 연동 성공", null)
        );
    }

    @ApiOperation(
            value = "월렛 계좌 연동 해제",
            notes = "월렛에 연동된 계좌를 해제합니다."
    )
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<Void>> unlinkAccount(
            @AuthenticationPrincipal Long userId,

            @PathVariable
            @Positive(message = "계좌 ID는 양수여야 합니다.")
            Long accountId
    ) {
        walletService.unlinkAccount(userId, accountId);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 계좌 연동 해제 성공", null)
        );
    }

    @ApiOperation(
            value = "월렛 대표 계좌 설정",
            notes = "월렛에 연동된 계좌 중 기본 충전 및 출금에 사용할 대표 계좌를 설정합니다."
    )
    @PutMapping("/accounts/{accountId}/primary")
    public ResponseEntity<ApiResponse<Void>> updatePrimaryAccount(
            @AuthenticationPrincipal Long userId,

            @PathVariable
            @Positive(message = "계좌 ID는 양수여야 합니다.")
            Long accountId
    ) {
        walletService.updatePrimaryAccount(userId, accountId);

        return ResponseEntity.ok(
                ApiResponse.success("월렛 대표 계좌 설정 성공", null)
        );
    }

    @ApiOperation(
            value = "월 목표 자동 송금 설정",
            notes = "매월 지정일에 연동 계좌에서 월렛으로 자동 송금되도록 설정합니다."
    )
    @GetMapping("/auto-saving")
    public ResponseEntity<ApiResponse<WalletAutoSavingResponseDto>> getAutoSavingRule(
            @AuthenticationPrincipal Long userId
    ) {
        WalletAutoSavingResponseDto data = walletService.getAutoSavingRule(userId);

        return ResponseEntity.ok(
                ApiResponse.success("월 목표 자동 송금 설정 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "월 목표 자동 송금 설정 수정",
            notes = "자동 송금 금액, 송금일, 출금 계좌, 활성 여부를 등록하거나 수정합니다."
    )
    @PutMapping("/auto-saving")
    public ResponseEntity<ApiResponse<Void>> updateAutoSavingRule(
            @AuthenticationPrincipal Long userId,

            @Valid
            @RequestBody
            WalletAutoSavingRequestDto request
    ) {
        walletService.updateAutoSavingRule(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("월 목표 자동 송금 설정 성공", null)
        );
    }

    @ApiOperation(
            value = "월 목표 자동 송금 해제",
            notes = "월렛에 설정된 월 목표 자동 송금을 해제합니다."
    )
    @DeleteMapping("/auto-saving")
    public ResponseEntity<ApiResponse<Void>> deleteAutoSavingRule(
            @AuthenticationPrincipal Long userId
    ) {
        walletService.deleteAutoSavingRule(userId);

        return ResponseEntity.ok(
                ApiResponse.success("월 목표 자동 송금 해제 성공", null)
        );
    }

    @ApiOperation(
            value = "자동 채우기 성공/실패 기록 조회",
            notes = "월 목표 자동 채우기가 실행될 때마다 남은 성공/실패 기록을 최신순으로 조회합니다."
    )
    @GetMapping("/auto-saving/logs")
    public ResponseEntity<ApiResponse<List<WalletAutoSavingLogResponseDto>>> getAutoSavingLogs(
            @AuthenticationPrincipal Long userId
    ) {
        List<WalletAutoSavingLogResponseDto> data = walletService.getAutoSavingLogs(userId);

        return ResponseEntity.ok(
                ApiResponse.success("자동 채우기 기록 조회 성공", data)
        );
    }

}

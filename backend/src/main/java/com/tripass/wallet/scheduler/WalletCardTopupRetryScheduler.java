package com.tripass.wallet.scheduler;

import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.dto.response.WalletCardTopupResponseDto;
import com.tripass.wallet.travelcard.enums.CardTopupStatus;
import com.tripass.wallet.travelcard.mapper.WalletTravelCardMapper;
import com.tripass.wallet.travelcard.service.WalletCardTopupRefundService;
import com.tripass.wallet.travelcard.service.WalletTravelCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 실패하거나 처리 중단된 트래블카드 충전 요청을 재시도하고 최종 실패 건을 환불 처리하는 스케줄러입니다. */

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletCardTopupRetryScheduler {

    private static final int TOPUP_RETRY_LIMIT = 30;
    private static final int MAX_RETRY_COUNT = 3;

    private final WalletTravelCardMapper walletTravelCardMapper;
    private final WalletTravelCardService walletTravelCardService;
    private final WalletCardTopupRefundService walletCardTopupRefundService;

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void retryPendingTopups() {
        List<WalletCardTopup> topups = walletTravelCardMapper.findRetryableTopups(TOPUP_RETRY_LIMIT);

        if (topups.isEmpty()) {
            log.debug("트래블카드 충전 재시도 대상 없음");
            return;
        }

        log.info("트래블카드 충전 재시도 시작 - 대상 건수: {}", topups.size());

        for (WalletCardTopup topup : topups) {
            retryOne(topup.getId());
        }
    }

    private void retryOne(Long topupId) {
        try {
            WalletCardTopupResponseDto response = walletTravelCardService.processTopup(topupId);

            if (CardTopupStatus.FAILED.name().equals(response.getStatus())
                    && response.getRetryCount() != null
                    && response.getRetryCount() >= MAX_RETRY_COUNT) {
                walletCardTopupRefundService.refund(topupId);
                log.warn("트래블카드 충전 최종 실패 환불 처리 - topupId: {}", topupId);
            }
        } catch (Exception e) {
            log.error("트래블카드 충전 재시도 실패 - topupId: {}, 사유: {}", topupId, e.getMessage(), e);
        }
    }
}

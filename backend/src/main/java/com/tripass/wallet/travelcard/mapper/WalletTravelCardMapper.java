package com.tripass.wallet.travelcard.mapper;

import com.tripass.wallet.fx.domain.WalletExchangeTransaction;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.TravelCardLedger;
import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import com.tripass.wallet.travelcard.dto.response.TravelCardCurrencyBalanceResponseDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardLedgerResponseDto;
import com.tripass.wallet.travelcard.dto.response.TravelCardTransactionResponseDto;
import com.tripass.wallet.travelcard.dto.response.UserTravelCardOptionResponseDto;
import com.tripass.wallet.travelcard.dto.response.WalletTravelCardResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/** 트래블카드 연동, 외화 잔액, 카드 충전 내역 데이터 접근 메서드를 정의하는 MyBatis Mapper입니다. */

@Mapper
public interface WalletTravelCardMapper {

    boolean existsTravelCardById(@Param("travelCardId") Long travelCardId);

    boolean existsSupportedCardCurrency(
            @Param("travelCardId") Long travelCardId,
            @Param("currencyCode") String currencyCode
    );

    boolean existsTravelCardLedgerIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    boolean existsWalletCardTopupIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    boolean existsUserTravelCard(
            @Param("userId") Long userId,
            @Param("userTravelCardId") Long userTravelCardId
    );

    List<UserTravelCardOptionResponseDto> findUserTravelCardOptions(@Param("userId") Long userId);

    WalletTravelCardResponseDto findLinkedTravelCardByWalletId(@Param("walletId") Long walletId);

    WalletTravelCard findActiveWalletTravelCardByWalletId(@Param("walletId") Long walletId);

    WalletTravelCard findWalletTravelCardForUpdate(
            @Param("walletId") Long walletId,
            @Param("walletTravelCardId") Long walletTravelCardId
    );

    int insertWalletTravelCard(WalletTravelCard walletTravelCard);

    int unlinkWalletTravelCard(@Param("walletId") Long walletId);

    List<TravelCardCurrencyBalanceResponseDto> findTravelCardBalances(@Param("walletTravelCardId") Long walletTravelCardId);

    List<TravelCardLedgerResponseDto> findTravelCardLedgers(@Param("walletTravelCardId") Long walletTravelCardId);

    List<TravelCardTransactionResponseDto> findTravelCardTransactions(
            @Param("walletId") Long walletId,
            @Param("walletTravelCardId") Long walletTravelCardId
    );

    TravelCardBalance findTravelCardBalanceForUpdate(
            @Param("walletTravelCardId") Long walletTravelCardId,
            @Param("currencyCode") String currencyCode
    );

    int insertTravelCardBalance(TravelCardBalance travelCardBalance);

    int updateTravelCardBalance(
            @Param("balanceId") Long balanceId,
            @Param("balanceAmount") BigDecimal balanceAmount,
            @Param("krwEstimatedAmount") BigDecimal krwEstimatedAmount
    );

    int insertTravelCardLedger(TravelCardLedger ledger);

    int insertWalletCardTopup(WalletCardTopup topup);

    WalletCardTopup findWalletCardTopupForUpdate(@Param("topupId") Long topupId);

    List<WalletCardTopup> findRetryableTopups(@Param("limit") int limit);

    int updateWalletCardTopupProcessing(@Param("topupId") Long topupId);

    int updateWalletCardTopupCompleted(
            @Param("topupId") Long topupId,
            @Param("walletLedgerId") Long walletLedgerId,
            @Param("cardLedgerId") Long cardLedgerId,
            @Param("externalTransactionId") String externalTransactionId,
            @Param("completedAt") java.time.LocalDateTime completedAt
    );

    int updateWalletCardTopupFailed(
            @Param("topupId") Long topupId,
            @Param("failureReason") String failureReason,
            @Param("failedAt") java.time.LocalDateTime failedAt
    );

    int updateWalletCardTopupRefunded(
            @Param("topupId") Long topupId,
            @Param("walletLedgerId") Long walletLedgerId
    );

    int insertWalletExchangeTransaction(WalletExchangeTransaction exchangeTransaction);

    BigDecimal findLatestDealBaseRate(@Param("currencyCode") String currencyCode);
}

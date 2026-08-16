package com.tripass.wallet.mapper;

import com.tripass.wallet.domain.*;
import com.tripass.wallet.dto.response.*;
import com.tripass.wallet.fx.domain.WalletExchangeTransaction;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.TravelCardLedger;
import com.tripass.wallet.travelcard.domain.WalletCardTopup;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 월렛, 월렛 연동 계좌, 월렛 원장, 자동 송금 설정 데이터 접근 메서드를 정의하는 MyBatis Mapper 인터페이스입니다. */

@Mapper
public interface WalletMapper {

    Wallet findWalletByUserIdForUpdate(@Param("userId") Long userId);

    Wallet findWalletByUserId(@Param("userId") Long userId);

    Wallet findWalletById(@Param("walletId") Long walletId);

    WalletMainResponseDto findWalletMainByUserId(@Param("userId") Long userId);

    List<WalletMonthlySavingResponseDto> findMonthlySavings(
            @Param("userId") Long userId,
            @Param("walletId") Long walletId
    );

    WalletMonthlySavingDetailResponseDto findMonthlySavingDetailSummary(
            @Param("walletId") Long walletId,
            @Param("month") String month
    );

    List<WalletLedgerResponseDto> findLedgersByWalletIdAndMonth(
            @Param("walletId") Long walletId,
            @Param("month") String month
    );

    WalletLinkedTravelCardResponseDto findLinkedTravelCardForMain(@Param("walletId") Long walletId);

    List<WalletForeignBalanceResponseDto> findForeignBalancesForMain(@Param("walletTravelCardId") Long walletTravelCardId);

    List<WalletForeignBalanceResponseDto> findAllForeignBalances(@Param("walletTravelCardId") Long walletTravelCardId);

    boolean existsWalletByUserId(@Param("userId") Long userId);

    int insertWallet(Wallet wallet);

    boolean existsLinkedAccount(
            @Param("walletId") Long walletId,
            @Param("accountId") Long accountId
    );

    boolean existsLinkedAccountByUserId(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId
    );

    boolean existsIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    WalletLedger findWalletLedgerByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    boolean existsTravelCardLedgerIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    boolean existsTravelCardById(@Param("travelCardId") Long travelCardId);

    boolean existsSupportedCardCurrency(
            @Param("travelCardId") Long travelCardId,
            @Param("currencyCode") String currencyCode
    );

    int insertWalletAccount(WalletAccount walletAccount);

    int unlinkWalletAccount(
            @Param("walletId") Long walletId,
            @Param("accountId") Long accountId
    );

    int resetPrimaryAccount(@Param("walletId") Long walletId);

    int updatePrimaryAccount(
            @Param("walletId") Long walletId,
            @Param("accountId") Long accountId
    );

    List<WalletAccountResponseDto> findAccountsByUserId(@Param("userId") Long userId);

    List<WalletAccountResponseDto> findAccountOptionsByUserId(@Param("userId") Long userId);

    WalletAccountResponseDto findPrimaryAccountByWalletId(@Param("walletId") Long walletId);

    int decreaseAccountBalance(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("amount") BigDecimal amount
    );

    int increaseAccountBalance(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("amount") BigDecimal amount
    );

    int updateWalletBalance(
            @Param("walletId") Long walletId,
            @Param("balanceAmount") java.math.BigDecimal balanceAmount,
            @Param("version") Long version
    );

    int insertWalletLedger(WalletLedger ledger);

    List<WalletLedgerResponseDto> findLedgersByWalletId(@Param("walletId") Long walletId);

    WalletAutoSavingRule findAutoSavingRuleByWalletId(@Param("walletId") Long walletId);

    List<WalletAutoSavingRule> findDueAutoSavingRules(@Param("baseDate") LocalDate baseDate);

    WalletAutoSavingResponseDto findAutoSavingRuleResponseByWalletId(@Param("walletId") Long walletId);

    int upsertAutoSavingRule(WalletAutoSavingRule rule);

    int deleteAutoSavingRule(@Param("walletId") Long walletId);

    int updateAutoSavingNextTransferDate(
            @Param("ruleId") Long ruleId,
            @Param("nextTransferDate") LocalDate nextTransferDate
    );

    WalletTravelCardResponseDto findLinkedTravelCardByWalletId(@Param("walletId") Long walletId);

    WalletTravelCard findWalletTravelCardByIdForUpdate(
            @Param("walletId") Long walletId,
            @Param("walletTravelCardId") Long walletTravelCardId
    );

    int insertWalletTravelCard(WalletTravelCard walletTravelCard);

    int unlinkWalletTravelCard(@Param("walletId") Long walletId);

    List<TravelCardCurrencyBalanceResponseDto> findTravelCardBalances(@Param("walletTravelCardId") Long walletTravelCardId);

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

    int insertWalletExchangeTransaction(WalletExchangeTransaction exchangeTransaction);

    BigDecimal findLatestDealBaseRate(@Param("currencyCode") String currencyCode);

    WalletTravelCard findActiveWalletTravelCardByWalletId(@Param("walletId") Long walletId);

    int insertAutoSavingLog(WalletAutoSavingLog log);

    List<WalletAutoSavingLogResponseDto> findAutoSavingLogsByWalletId(@Param("walletId") Long walletId);
}

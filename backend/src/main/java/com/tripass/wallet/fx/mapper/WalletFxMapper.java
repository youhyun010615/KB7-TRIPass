package com.tripass.wallet.fx.mapper;

import com.tripass.wallet.fx.domain.WalletExchangeTransaction;
import com.tripass.wallet.fx.dto.response.WalletCurrencyResponseDto;
import com.tripass.wallet.travelcard.domain.TravelCardBalance;
import com.tripass.wallet.travelcard.domain.TravelCardLedger;
import com.tripass.wallet.travelcard.domain.WalletTravelCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/** 월렛 환전, 트래블카드 외화 잔액, 환전 원장 데이터 접근 메서드를 정의하는 MyBatis Mapper입니다. */

@Mapper
public interface WalletFxMapper {

    List<WalletCurrencyResponseDto> findCurrencies();

    BigDecimal findLatestDealBaseRate(@Param("currencyCode") String currencyCode);

    WalletTravelCard findWalletTravelCardForUpdate(
            @Param("walletId") Long walletId,
            @Param("walletTravelCardId") Long walletTravelCardId
    );

    TravelCardBalance findTravelCardBalanceForUpdate(
            @Param("walletTravelCardId") Long walletTravelCardId,
            @Param("currencyCode") String currencyCode
    );

    int updateTravelCardBalance(
            @Param("balanceId") Long balanceId,
            @Param("balanceAmount") BigDecimal balanceAmount,
            @Param("krwEstimatedAmount") BigDecimal krwEstimatedAmount
    );

    int insertTravelCardLedger(TravelCardLedger ledger);

    boolean existsTravelCardLedgerIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    int insertWalletExchangeTransaction(WalletExchangeTransaction exchangeTransaction);
}

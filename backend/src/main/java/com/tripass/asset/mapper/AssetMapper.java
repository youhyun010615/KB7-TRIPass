package com.tripass.asset.mapper;

import com.tripass.asset.dto.*;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AssetMapper {
    // codef_connections
    void insertCodefConnection(CodefConnectionDto dto);
    CodefConnectionDto findConnectionByUserId(Long userId);

    // codef_connected_institutions
    void insertConnectedInstitution(CodefConnectedInstitutionDto dto);
    CodefConnectedInstitutionDto findConnectedInstitution(
            @org.apache.ibatis.annotations.Param("codefConnectionId") Long codefConnectionId,
            @org.apache.ibatis.annotations.Param("organizationCode") String organizationCode,
            @org.apache.ibatis.annotations.Param("businessType") String businessType
    );

    // accounts
    void insertAccount(AccountDto dto);
    void updateAccountOnReconnect(AccountDto dto);
    AccountDto findAccountByUserIdAndNumber(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("organizationCode") String organizationCode,
            @org.apache.ibatis.annotations.Param("accountNumber") String accountNumber
    );
    AccountDto findAccountByUserIdAndNumberOnly(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("accountNumber") String accountNumber
    );
    int linkCardsToAccountByPaymentNumber(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("accountId") Long accountId,
            @org.apache.ibatis.annotations.Param("accountNumber") String accountNumber
    );
    List<AccountDto> findAccountsByUserId(Long userId);

    void deleteAccount(
            @org.apache.ibatis.annotations.Param("accountId") Long accountId,
            @org.apache.ibatis.annotations.Param("userId") Long userId
    );

    // transactions
    void insertTransaction(TransactionDto dto);
    List<TransactionDto> findTransactionsByAccountId(Long accountId);
    AccountDto findAccountById(
            @org.apache.ibatis.annotations.Param("accountId") Long accountId,
            @org.apache.ibatis.annotations.Param("userId") Long userId
    );

    //개별 계좌 거래내역 조회(날짜, 타입 필터)
    List<TransactionDto> findTransactionsByAccountIdWithFilter(
            @org.apache.ibatis.annotations.Param("accountId") Long accountId,
            @org.apache.ibatis.annotations.Param("startDate") LocalDate startDate,
            @org.apache.ibatis.annotations.Param("endDate") LocalDate endDate,
            @org.apache.ibatis.annotations.Param("type") String type
    );

    //거래 단건 상세 조회
    TransactionDto findTransactionById(
      @org.apache.ibatis.annotations.Param("transactionId") Long transactionId,
      @org.apache.ibatis.annotations.Param("userId") Long userId
    );

    //전체 계좌 거래내역 조회
    List<TransactionDto> findTransactionsByUserId(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("startDate") LocalDate startDate,
            @org.apache.ibatis.annotations.Param("endDate") LocalDate endDate
    );

    void updateTransaction(
            @org.apache.ibatis.annotations.Param("transactionId") Long transactionId,
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("categoryId") Long categoryId,
            @org.apache.ibatis.annotations.Param("memo") String memo
    );

    // supported_institutions
    List<SupportedInstitutionDto> findSupportedInstitutionsByBusinessType(
            @org.apache.ibatis.annotations.Param("businessType") String businessType
    );

    // cards
    void insertCard(CardDto dto);
    void updateCardOnReconnect(CardDto dto);
    List<CardDto> findCardsByUserId(Long userId);
    CardDto findCardByIdAndUserId(
            @org.apache.ibatis.annotations.Param("cardId") Long cardId,
            @org.apache.ibatis.annotations.Param("userId") Long userId
    );
    void deleteCard(
            @org.apache.ibatis.annotations.Param("cardId") Long cardId,
            @org.apache.ibatis.annotations.Param("userId") Long userId
    );
    void deleteUserTravelCardByCard(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("maskedCardNumber") String maskedCardNumber
    );
    CardDto findCardByUserIdAndNumber(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("maskedCardNumber") String maskedCardNumber
    );
    void upsertUserTravelCardFromLinkedCard(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("cardName") String cardName,
            @org.apache.ibatis.annotations.Param("maskedCardNumber") String maskedCardNumber,
            @org.apache.ibatis.annotations.Param("organizationCode") String organizationCode
    );

    // transactions (카드 — 중복 시 merchant_name 업데이트, 신규 시 INSERT)
    void upsertTransactionFromCard(TransactionDto dto);
    void deleteTransactionByExternalKey(@org.apache.ibatis.annotations.Param("externalKey") String externalKey);
    Long findCategoryIdByCode(@org.apache.ibatis.annotations.Param("categoryCode") String categoryCode);
    List<TransactionDto> findUnclassifiedCardTransactionsByUserId(Long userId);
    int updateAutoClassification(TransactionDto dto);

    // 카드별 거래내역 조회
    List<TransactionDto> findTransactionsByCardId(
            @org.apache.ibatis.annotations.Param("cardId") Long cardId,
            @org.apache.ibatis.annotations.Param("startDate") LocalDate startDate,
            @org.apache.ibatis.annotations.Param("endDate") LocalDate endDate
    );
    void updateCardLastSyncedAt(@org.apache.ibatis.annotations.Param("cardId") Long cardId);

    //거래내역 캘린더 조회
    List<CalendarDayDto> findCalendarByMonth(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("year") int year,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("type") String type
    );

}

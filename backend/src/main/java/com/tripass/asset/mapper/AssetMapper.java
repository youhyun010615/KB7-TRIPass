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

    // accounts
    void insertAccount(AccountDto dto);
    void updateAccountOnReconnect(AccountDto dto);
    AccountDto findAccountByUserIdAndNumber(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
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
    List<SupportedInstitutionDto> findAllSupportedInstitutions();

    // cards
    void insertCard(CardDto dto);
    List<CardDto> findCardsByUserId(Long userId);
    CardDto findCardByIdAndUserId(
            @org.apache.ibatis.annotations.Param("cardId") Long cardId,
            @org.apache.ibatis.annotations.Param("userId") Long userId
    );

    // transactions (카드 — 중복 시 merchant_name 업데이트, 신규 시 INSERT)
    void upsertTransactionFromCard(TransactionDto dto);

    // 카드별 거래내역 조회
    List<TransactionDto> findTransactionsByCardId(
            @org.apache.ibatis.annotations.Param("cardId") Long cardId,
            @org.apache.ibatis.annotations.Param("startDate") LocalDate startDate,
            @org.apache.ibatis.annotations.Param("endDate") LocalDate endDate
    );

    //거래내역 캘린더 조회
    List<CalendarDayDto> findCalendarByMonth(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("year") int year,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("type") String type
    );

}
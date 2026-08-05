package com.tripass.asset.mapper;

import com.tripass.asset.dto.*;
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
    List<AccountDto> findAccountsByUserId(Long userId);

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
            @org.apache.ibatis.annotations.Param("startDate") String startDate,
            @org.apache.ibatis.annotations.Param("endDate") String endDate,
            @org.apache.ibatis.annotations.Param("type") String type
    );

    //거래 단건 상세 조회
    TransactionDto findTransactionById(
      @org.apache.ibatis.annotations.Param("transactionId") Long transactionId,
      @org.apache.ibatis.annotations.Param("userId") Long userId
    );

    //전체 계좌 거래내역 조회
    List<TransactionDto> findTransactionsByUserId(Long userId);

    // supported_institutions
    List<SupportedInstitutionDto> findAllSupportedInstitutions();
}
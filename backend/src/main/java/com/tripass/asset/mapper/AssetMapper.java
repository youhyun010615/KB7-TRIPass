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
    AccountDto findAccountById(@org.apache.ibatis.annotations.Param("accountId") Long accountId,
                               @org.apache.ibatis.annotations.Param("userId") Long userId);

    // supported_institutions
    List<SupportedInstitutionDto> findAllSupportedInstitutions();
}
package com.tripass.asset.mapper;

import com.tripass.asset.dto.*;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AssetMapper {
    void insertCodefConnection(CodefConnectionDto dto);
    CodefConnectionDto findConnectionByUserId(Long userId);

    void insertConnectedInstitution(CodefConnectedInstitutionDto dto);

    void insertAccount(AccountDto dto);
    List<AccountDto> findAccountsByUserId(Long userId);

    List<SupportedInstitutionDto> findAllSupportedInstitutions();
}
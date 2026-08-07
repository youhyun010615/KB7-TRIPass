package com.tripass.bank.mapper;

import com.tripass.bank.domain.ExchangeBankBranch;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BankMapper {
    /**
     * 기존 지점 정보 모두 삭제
     */
    void deleteAllBranches();

    /**
     * 지점 정보 일괄 삽입
     */
    void insertBankBranches(List<ExchangeBankBranch> branches);
}

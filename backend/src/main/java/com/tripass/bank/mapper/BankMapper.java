package com.tripass.bank.mapper;

import com.tripass.bank.domain.ExchangeBankBranch;
import com.tripass.bank.dto.BankBranchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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

    /**
     * 반경 내 은행 지점 검색
     */
    List<BankBranchDto> findNearbyBanks(@Param("lat") BigDecimal lat, @Param("lng") BigDecimal lng, @Param("radius") double radius);

    /**
     * 지점 상세 조회
     */
    BankBranchDto findById(@Param("id") Long id);

    /**
     * 전체 또는 키워드 기반 검색
     */
    List<BankBranchDto> findAllByKeyword(@Param("keyword") String keyword);
}

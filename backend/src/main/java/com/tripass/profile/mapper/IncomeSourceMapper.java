package com.tripass.profile.mapper;

import com.tripass.profile.mapper.IncomeSourceMapper;
import com.tripass.profile.model.IncomeSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

//income_source 테이블에 접근하는 Mapper
@Mapper
public interface IncomeSourceMapper {

    //급여 정보 등록
    int insertIncomeSource(IncomeSource incomeSource);

    //로그인 회원의 활성 급여 목록 조회
    List<IncomeSource> findAllByUserId(
            @Param("userId") Long userId
    );

    //로그인 회원의 급여 단건 조회
    //급여 ID와 회원 ID를 함께 검사하여
    //다른 회원의 급여 정보 접근을 방지한다.
    IncomeSource findByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    //로그인 회원의 활성 급여 합계 조회
    BigDecimal sumGrossAmountByUserId(
            @Param("userId") Long userId
    );

    //급여 정보 수정
    int updateIncomeSource(IncomeSource incomeSource);

    //급여 정보 논리 삭제
    int softDeleteIncomeSource(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    //급여 등록·수정에 사용할 계좌가
    //로그인 회원 소유의 활성 계좌인지 확인
    int countActiveAccountByIdAndUserId(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId
    );
}

package com.tripass.saving.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 월간 소비 분석과 주간 미션 판정의 자동 처리 대상을 조회합니다. */
@Mapper
public interface SavingAutomationMapper {

    /** 분석월에 분류된 출금 거래가 존재하는 활성 사용자를 조회합니다. */
    List<Long> findMonthlyAnalysisTargetUserIds(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    /** 판정 또는 보상 재처리가 필요한 주간 미션 보유 사용자를 조회합니다. */
    List<Long> findWeeklyEvaluationTargetUserIds(
            @Param("targetYearMonth") String targetYearMonth,
            @Param("weekNumber") int weekNumber,
            @Param("runDate") LocalDate runDate
    );
}

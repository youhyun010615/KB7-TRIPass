package com.tripass.saving.service;

import com.tripass.common.config.RootConfig;
import com.tripass.common.config.SecurityConfig;
import com.tripass.saving.dto.MissionOptionResponseDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
import com.tripass.saving.mapper.MissionCategorySelectionMapper;
import com.tripass.saving.mapper.MonthlySpendingAnalysisMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * 실제 운영 설정인 {@link RootConfig}를 그대로 로딩해, MissionCategorySelectionService와
 * {@link com.tripass.saving.analysis.MissionReductionCalculator} 생성자 주입이 실제 Spring
 * 컨테이너에서 정상 동작하는지 검증한다. 방식은 {@code MonthlySpendingAnalysisSpringWiringTest}(#210)와
 * 동일하다 — DataSource·Mapper만 목 빈으로 대체하고 나머지는 프로덕션과 동일하게 조립한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RootConfig.class,
        SecurityConfig.class,
        MissionCategorySelectionSpringWiringTest.DataSourceOverride.class,
        MissionCategorySelectionSpringWiringTest.MapperOverride.class
})
class MissionCategorySelectionSpringWiringTest {

    @Autowired
    private MissionCategorySelectionService missionCategorySelectionService;

    @Autowired
    private MonthlySpendingAnalysisMapper monthlySpendingAnalysisMapper;

    @Autowired
    private MissionCategorySelectionMapper missionCategorySelectionMapper;

    @Test
    @DisplayName("RootConfig를 실제로 로딩해도 Service 빈이 정상 주입된다")
    void serviceBeanIsWired() {
        assertNotNull(missionCategorySelectionService);
    }

    @Test
    @DisplayName("실제 RootConfig로 조립된 Service가 전체 파이프라인을 NPE 없이 끝까지 실행한다")
    void getMissionOptions_runsThroughRealSpringWiredBeans() {
        // MissionReductionCalculator 빈이 주입 안 됐다면(null) 이 호출에서 NPE가 나야 정상이다.
        reset(monthlySpendingAnalysisMapper, missionCategorySelectionMapper);
        Long userId = 1L;
        YearMonth analysisMonth = YearMonth.of(2026, 7);

        MonthlySpendingAnalysisDto analysis = new MonthlySpendingAnalysisDto();
        analysis.setId(100L);
        when(monthlySpendingAnalysisMapper.findMonthlyAnalysis(any(), anyString())).thenReturn(analysis);
        when(monthlySpendingAnalysisMapper.findRecommendedCategoryAnalyses(any())).thenReturn(List.of());

        List<MissionOptionResponseDto> result = missionCategorySelectionService.getMissionOptions(userId, analysisMonth);

        assertNotNull(result);
    }

    @Configuration
    static class DataSourceOverride {
        @Bean
        public DataSource dataSource() throws SQLException {
            // 트랜잭션 매니저가 커넥션을 요구하므로, 아무 동작도 하지 않는 목 커넥션을 반환한다.
            DataSource dataSource = Mockito.mock(DataSource.class);
            Connection connection = Mockito.mock(Connection.class);
            when(dataSource.getConnection()).thenReturn(connection);
            return dataSource;
        }
    }

    @Configuration
    static class MapperOverride {
        @Bean
        public MonthlySpendingAnalysisMapper monthlySpendingAnalysisMapper() {
            return Mockito.mock(MonthlySpendingAnalysisMapper.class);
        }

        @Bean
        public MissionCategorySelectionMapper missionCategorySelectionMapper() {
            return Mockito.mock(MissionCategorySelectionMapper.class);
        }
    }
}

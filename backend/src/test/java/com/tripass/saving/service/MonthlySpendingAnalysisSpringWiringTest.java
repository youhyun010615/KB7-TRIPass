package com.tripass.saving.service;

import com.tripass.common.config.RootConfig;
import com.tripass.common.config.SecurityConfig;
import com.tripass.saving.dto.MonthlyAnalysisResponseDto;
import com.tripass.saving.dto.MonthlySpendingAnalysisDto;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * 실제 운영 설정인 {@link RootConfig}를 그대로 로딩해, com.tripass.saving.service/analysis와
 * com.tripass.asset.duplicate에 대한 @ComponentScan·@MapperScan·생성자 주입이 실제 Spring 컨테이너에서
 * 정상 동작하는지 검증한다.
 *
 * DataSource(HikariCP)는 실제 DB 연결 없이는 생성 자체가 실패하므로, {@link DataSourceOverride}로
 * 같은 빈 이름("dataSource")을 나중에 등록해 RootConfig 원본 빈 정의를 대체한다(plain Spring의
 * 빈 재정의 규칙: 뒤에 등록된 정의가 앞선 정의를 덮어쓴다). MyBatis SqlSessionFactory는 DataSource를
 * 저장만 하고 실제 연결은 세션을 열 때 비로소 시도하므로, 목 DataSource만으로 빈 생성까지는 통과한다.
 * saving 도메인 Mapper는 실제 쿼리를 실행하지 않도록 별도로 목 빈으로 대체한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RootConfig.class,
        SecurityConfig.class,
        MonthlySpendingAnalysisSpringWiringTest.DataSourceOverride.class,
        MonthlySpendingAnalysisSpringWiringTest.MapperOverride.class
})
class MonthlySpendingAnalysisSpringWiringTest {

    @Autowired
    private MonthlySpendingAnalysisService monthlySpendingAnalysisService;

    @Autowired
    private MonthlySpendingAnalysisMapper mapper;

    @Test
    @DisplayName("RootConfig를 실제로 로딩해도 Service 빈이 정상 주입된다")
    void serviceBeanIsWired() {
        assertNotNull(monthlySpendingAnalysisService);
    }

    @Test
    @DisplayName("실제 RootConfig로 조립된 Service가 전체 파이프라인을 NPE 없이 끝까지 실행한다")
    void generateMonthlyAnalysis_runsThroughRealSpringWiredBeans() {
        // 계산기·중복 매처 빈 중 하나라도 주입이 안 됐다면(null) 이 호출에서 NPE가 나야 정상이다.
        reset(mapper);
        Long userId = 1L;
        YearMonth analysisMonth = YearMonth.of(2026, 7);

        when(mapper.findCategoryIdByCode(anyString())).thenReturn(1L);
        when(mapper.findMonthlyAnalysis(any(), anyString())).thenReturn(null, new MonthlySpendingAnalysisDto());
        when(mapper.findAccountWithdrawalTransactions(any(), any(), any())).thenReturn(List.of());
        when(mapper.findCheckCardWithdrawalTransactions(any(), any(), any())).thenReturn(List.of());
        when(mapper.findCreditCardWithdrawalTransactions(any(), any(), any())).thenReturn(List.of());
        when(mapper.findEarliestConnectionDate(any())).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(mapper.findActiveSavingTargetAmount(any())).thenReturn(new BigDecimal("700000"));
        when(mapper.findCategoryAnalyses(any())).thenReturn(List.of());
        when(mapper.findRecommendedCategoryAnalyses(any())).thenReturn(List.of());

        // 이 테스트의 목적은 응답 필드 값 검증(이미 MonthlySpendingAnalysisServiceTest에서 충분히 검증)이 아니라,
        // 실제 Spring이 조립한 계산기·매처 빈들로 예외 없이 전체 파이프라인이 끝까지 도는지 확인하는 것이다.
        MonthlyAnalysisResponseDto response = monthlySpendingAnalysisService.generateMonthlyAnalysis(userId, analysisMonth);

        assertNotNull(response);
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
    }
}

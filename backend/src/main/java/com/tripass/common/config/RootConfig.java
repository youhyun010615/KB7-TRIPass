package com.tripass.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * Root ApplicationContext
 * DataSource(HikariCP) · MyBatis · TransactionManager 설정
 */
@Configuration
@ComponentScan(basePackages = {
        "com.tripass.auth.service",
        "com.tripass.mypage.service",
        "com.tripass.profile.service",
        "com.tripass.financial.service",
        "com.tripass.asset.service",
        "com.tripass.saving.service",
        "com.tripass.saving.classification",
        "com.tripass.saving.analysis",
        "com.tripass.asset.duplicate",
        "com.tripass.checklist.service",
        "com.tripass.travel.service",
        "com.tripass.travel.client",
        "com.tripass.exchange.service",
        "com.tripass.exchange.client",
        "com.tripass.exchange.scheduler",
        "com.tripass.bank.service",
        "com.tripass.bank.client",
        "com.tripass.prepay.service",
        "com.tripass.schedule.service",
        "com.tripass.expense.service",
        "com.tripass.ocr.service",
        "com.tripass.ocr.client",
        "com.tripass.report.service",
        "com.tripass.auth.security",
        "com.tripass.common.util",
        "com.tripass.common.scheduler",
        "com.tripass.common.config",
        "com.tripass.auth.client",
})
@MapperScan(basePackages = {
        "com.tripass.auth.mapper",
        "com.tripass.mypage.mapper",
        "com.tripass.profile.mapper",
        "com.tripass.financial.mapper",
        "com.tripass.checklist.mapper",
        "com.tripass.asset.mapper",
        "com.tripass.saving.mapper",
        "com.tripass.travel.mapper",
        "com.tripass.exchange.mapper",
        "com.tripass.bank.mapper",
        "com.tripass.prepay.mapper",
        "com.tripass.schedule.mapper",
        "com.tripass.expense.mapper",
        "com.tripass.ocr.mapper",
        "com.tripass.report.mapper"
})
@PropertySource(value = {
        "classpath:application.properties",
        "classpath:application-local.properties"
}, ignoreResourceNotFound = true)
@EnableTransactionManagement
@EnableScheduling
public class RootConfig {

    @Resource
    private Environment env;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        // log4jdbc 드라이버 스파이로 SQL 로그 출력
        config.setDriverClassName("net.sf.log4jdbc.sql.jdbcapi.DriverSpy");
        config.setJdbcUrl(env.getProperty("db.url"));
        config.setUsername(env.getProperty("db.username"));
        config.setPassword(env.getProperty("db.password"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // 카카오 OAuth API 호출 전용 RestTemplate
    @Bean("kakaoRestTemplate")
    public RestTemplate kakaoRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);

        return new RestTemplate(requestFactory);
    }

    // Google OAuth API 호출 전용 RestTemplate
    @Bean("googleRestTemplate")
    public RestTemplate googleRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);

        return new RestTemplate(requestFactory);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfigLocation(
                new PathMatchingResourcePatternResolver()
                        .getResource("classpath:mybatis-config.xml")
        );
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/**/*.xml")
        );
        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}

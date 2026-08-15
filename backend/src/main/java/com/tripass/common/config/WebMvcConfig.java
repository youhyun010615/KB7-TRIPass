package com.tripass.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;

import java.util.List;

/**
 * Web ApplicationContext — Spring MVC 설정
 * CORS 는 SecurityConfig 에서 Spring Security 레벨로 처리
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "com.tripass.auth.controller",
        "com.tripass.mypage.controller",
        "com.tripass.profile.controller",
        "com.tripass.financial.controller",
        "com.tripass.asset.controller",
        "com.tripass.saving.controller",
        "com.tripass.travel.controller",
        "com.tripass.checklist.controller",
        "com.tripass.exchange.controller",
        "com.tripass.bank.controller",
        "com.tripass.prepay.controller",
        "com.tripass.schedule.controller",
        "com.tripass.expense.controller",
        "com.tripass.ocr.controller",
        "com.tripass.report.controller",
        "com.tripass.wallet.controller",
        "com.tripass.wallet.fx.controller",
        "com.tripass.wallet.travelcard.controller",
        "com.tripass.common.exception"
})
public class WebMvcConfig implements WebMvcConfigurer {

    /** JSON 및 이미지 byte[] 메시지 컨버터 등록 */
    @Override
    public void configureMessageConverters(
            List<HttpMessageConverter<?>> converters
    ) {
        // 이미지 파일의 byte[] 응답을 처리한다.
        converters.add(
                new ByteArrayHttpMessageConverter()
        );

        // ApiResponse 등의 JSON 응답을 처리한다.
        // 기본 ObjectMapper는 LocalDate/LocalDateTime/OffsetDateTime을 타임스탬프(숫자)로
        // 직렬화해 프론트엔드가 파싱할 수 없으므로, ISO-8601 문자열로 내려주도록 명시적으로 설정한다.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        converters.add(
                new MappingJackson2HttpMessageConverter(objectMapper)
        );
    }

    /** 정적 리소스 서빙 허용 */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    /** Swagger UI 정적 리소스 경로 등록 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /** Bean Validation validator 등록 */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /** 메서드 파라미터의 검증 애노테이션 처리 */
    @Bean
    public MethodValidationPostProcessor
    methodValidationPostProcessor() {
        MethodValidationPostProcessor processor =
                new MethodValidationPostProcessor();

        processor.setValidator(validator());

        return processor;
    }

    /** Spring MVC 요청값 검증에 사용할 Validator 설정 */
    @Override
    public Validator getValidator() {
        return validator();
    }

    /**
     * multipart/form-data 파일 업로드 요청을 처리한다.
     */
    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver =
                new StandardServletMultipartResolver();

        resolver.setResolveLazily(true);

        return resolver;
    }
}

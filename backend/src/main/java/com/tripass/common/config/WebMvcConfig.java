package com.tripass.common.config;

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
        "com.tripass.exchange.controller",
        "com.tripass.bank.controller",
        "com.tripass.prepay.controller",
        "com.tripass.schedule.controller",
        "com.tripass.expense.controller",
        "com.tripass.ocr.controller",
        "com.tripass.report.controller",
        "com.tripass.common.exception"
})
public class WebMvcConfig implements WebMvcConfigurer {

    /** JSON 메시지 컨버터 등록 */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
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

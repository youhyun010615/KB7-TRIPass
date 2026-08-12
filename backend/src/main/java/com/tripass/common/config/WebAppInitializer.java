package com.tripass.common.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

import javax.servlet.Filter;

/**
 * web.xml 대체 — Spring MVC DispatcherServlet 진입점
 * Tomcat 9 기동 시 자동으로 감지되어 실행됨
 */
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    private static final long MAX_UPLOAD_FILE_SIZE =
            10L * 1024L * 1024L;

    private static final long MAX_UPLOAD_REQUEST_SIZE =
            11L * 1024L * 1024L;

    /** Root ApplicationContext: DataSource, MyBatis, Service, Transaction */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{ RootConfig.class, SecurityConfig.class };
    }

    /** Web ApplicationContext: Controller, MVC 설정 */
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{
                WebMvcConfig.class,
                SwaggerConfig.class
        };
    }

    /** DispatcherServlet 이 처리할 URL 패턴 */
    @Override
    protected String[] getServletMappings() {
        return new String[]{ "/" };
    }

    /** 공통 필터: UTF-8 인코딩 */
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[]{ encodingFilter };
    }

    /**
     * DispatcherServlet에 multipart 업로드 제한을 설정한다.
     */
    @Override
    protected void customizeRegistration(
            ServletRegistration.Dynamic registration
    ) {
        MultipartConfigElement multipartConfig =
                new MultipartConfigElement(
                        System.getProperty("java.io.tmpdir"),
                        MAX_UPLOAD_FILE_SIZE,
                        MAX_UPLOAD_REQUEST_SIZE,
                        0
                );

        registration.setMultipartConfig(
                multipartConfig
        );
    }
}

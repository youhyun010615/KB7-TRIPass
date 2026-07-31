package com.tripass.common.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;

/**
 * web.xml 대체 — Spring MVC DispatcherServlet 진입점
 * Tomcat 9 기동 시 자동으로 감지되어 실행됨
 */
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    /** Root ApplicationContext: DataSource, MyBatis, Service, Transaction */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{ RootConfig.class, SecurityConfig.class };
    }

    /** Web ApplicationContext: Controller, MVC 설정 */
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{ WebMvcConfig.class };
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
}

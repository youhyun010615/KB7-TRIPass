package com.tripass.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tripass.auth.security.JwtAuthenticationFilter;
import com.tripass.auth.security.handler.JwtAccessDeniedHandler;
import com.tripass.auth.security.handler.JwtAuthenticationEntryPoint;

import java.util.List;

/**
 * Spring Security 설정
 *
 * TODO [송형진 - AUTH]: JWT 필터 추가 및 인증 필요 엔드포인트 설정
 *   1. JwtAuthenticationFilter 구현 후 .addFilterBefore() 로 등록
 *   2. .anyRequest().permitAll() → .anyRequest().authenticated() 로 변경
 *   3. /api/v1/auth/** 는 permitAll 유지
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler)

                )
                .authorizeHttpRequests(auth ->
                        auth
                                // 인증 API는 항상 허용
                                .antMatchers("/api/v1/auth/**").permitAll()
                                .antMatchers("/swagger-ui.html", "/v2/api-docs", "/webjars/**",
                                        "/swagger-resources/**").permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Vue 개발 서버 주소 — 빌드 배포 시 실제 도메인으로 변경
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:5174"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

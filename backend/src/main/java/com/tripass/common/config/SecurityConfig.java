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
import org.springframework.http.HttpMethod;
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
                // Access Token은 Authorization 헤더로 전달하고 세션은 STATELESS로 관리한다.
                // Refresh Token 쿠키는 SameSite=Lax이므로 크로스사이트 POST에는 전송되지 않는다.
                // SameSite=None으로 변경한다면 CSRF 보호를 다시 검토해야 한다.
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
                                // 로그인 상태 비밀번호 변경은 Access Token 필요
                                .antMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/auth/password/change"
                                ).authenticated()

                                // 나머지 회원가입·로그인·인증 API는 비로그인 접근 허용
                                .antMatchers("/api/v1/auth/**").permitAll()

                                .antMatchers(
                                        "/swagger-ui.html",
                                        "/v2/api-docs",
                                        "/webjars/**",
                                        "/swagger-resources/**"
                                ).permitAll()

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
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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

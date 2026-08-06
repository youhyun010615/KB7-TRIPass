package com.tripass.auth.security;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

//요청의 JWT AccessToken을 검증하고 SpringSecurity 인증 정보를 등록하는 필터
@Component
@RequiredArgsConstructor
@Log4j2
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private static final String AUTHORIZATION_HEADER ="Authorization";
    private static final String BEARER_PREFIX ="Bearer ";
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = resolveAccessToken(request);

        //이미 인증정보 등록한 요청은 다시 처리 안함, 토큰 유효성 검사
        if(accessToken !=null && SecurityContextHolder.getContext().getAuthentication() == null){
            authenticate(accessToken, request);
        }
        filterChain.doFilter(request, response);
    }
    //유효한 AccessToken이면 Spring Security 인증 정보 등록
    private void authenticate(String accessToken, HttpServletRequest request){
        try{
            if(!jwtTokenProvider.validateAccessToken(accessToken)){
                return;
            }
            Long userId = jwtTokenProvider.getUserId(accessToken);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException exception){
            SecurityContextHolder.clearContext();

            log.debug(
                    "JWT 인증 정보 등록 실패: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }
    //Authorization 헤더에서 Bearer Token 추출
    private String resolveAccessToken(HttpServletRequest request){
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if(authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(
                BEARER_PREFIX.length()
        );
        if(token.isBlank()){
            return null;
        }
        return token;
    }
}

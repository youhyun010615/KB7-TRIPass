package com.tripass.auth.security.handler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripass.common.response.ApiResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//인증되지 않은 사용자가 보호된 API 접근 시 401 JSON 응답 반환
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.setContentType("application/json; charset=UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(
                "AUTH_UNAUTHORIZED",
                "로그인이 필요합니다."
        );

        objectMapper.writeValue(response.getWriter(), errorResponse);

    }
}

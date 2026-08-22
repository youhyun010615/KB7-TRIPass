package com.tripass.auth.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripass.common.response.ApiResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//로그인은 하였으나 요청 기능에 대한 권한이 없을 때 403 Json을 반환
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );
        response.setContentType(
                "application/json;charset=UTF-8"
        );
        ApiResponse<Void> errorResponse = ApiResponse.error(
                "AUTH_FORBIDDEN",
                "해당 기능에 접근할 권한이 없습니다."
        );
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

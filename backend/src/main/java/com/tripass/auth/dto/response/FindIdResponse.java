package com.tripass.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FindIdResponse {

    // 백엔드에서 마스킹한 로그인 아이디 목록
    private List<String> maskedLoginIds;
}
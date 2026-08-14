package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

// 프론트에서 전달받은 카카오 인가 코드
@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    @NotBlank(message = "카카오 인가 코드를 입력해 주세요.")
    private String code;
}
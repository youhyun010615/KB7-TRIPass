package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

// 프론트에서 전달받은 Google 인가 코드와 OAuth state
@Getter
@NoArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Google 인가 코드를 입력해 주세요.")
    private String code;

    @NotBlank(message = "Google OAuth state를 입력해 주세요.")
    private String state;
}
package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangePasswordRequest {

    // 현재 비밀번호
    private String currentPassword;

    // 변경할 새 비밀번호
    private String newPassword;

    // 새 비밀번호 확인
    private String newPasswordConfirm;
}
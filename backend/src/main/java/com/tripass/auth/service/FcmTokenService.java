package com.tripass.auth.service;

import com.tripass.auth.dto.request.FcmTokenRequestDto;

public interface FcmTokenService {
    void registerToken(Long userId, FcmTokenRequestDto requestDto);
}

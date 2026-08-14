package com.tripass.auth.service;

import com.tripass.auth.dto.request.FcmTokenRequestDto;
import com.tripass.auth.mapper.UserFcmTokenMapper;
import com.tripass.auth.model.UserFcmToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenServiceImpl implements FcmTokenService {

    private final UserFcmTokenMapper fcmTokenMapper;

    @Override
    @Transactional
    public void registerToken(Long userId, FcmTokenRequestDto requestDto) {
        UserFcmToken token = UserFcmToken.builder()
                .userId(userId)
                .deviceToken(requestDto.getDeviceToken())
                .deviceType(requestDto.getDeviceType())
                .build();
        
        fcmTokenMapper.saveToken(token);
    }
}

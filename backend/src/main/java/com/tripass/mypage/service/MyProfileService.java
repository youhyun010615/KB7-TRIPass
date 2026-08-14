package com.tripass.mypage.service;

import com.tripass.mypage.dto.MyProfileResponse;
import com.tripass.mypage.dto.UpdateMyProfileRequest;

public interface MyProfileService {

    // 로그인한 회원의 회원정보 조회
    MyProfileResponse getMyProfile(Long userId);

    // 로그인한 회원의 이름 변경 후 최신 회원정보 반환
    MyProfileResponse updateMyProfile(
            Long userId,
            UpdateMyProfileRequest request
    );
}
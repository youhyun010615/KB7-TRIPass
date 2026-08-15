package com.tripass.mypage.service;

import com.tripass.common.exception.CustomException;
import com.tripass.mypage.dto.MyProfileResponse;
import com.tripass.mypage.dto.UpdateMyProfileRequest;
import com.tripass.mypage.mapper.MyProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyProfileServiceImpl implements MyProfileService {

    private static final int MAX_NAME_LENGTH = 100;

    private final MyProfileMapper myProfileMapper;

    @Override
    public MyProfileResponse getMyProfile(Long userId) {
        validateUserId(userId);

        MyProfileResponse profile =
                myProfileMapper.findMyProfileByUserId(userId);

        if (profile == null) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "MYP_USER_NOT_FOUND",
                    "회원정보를 찾을 수 없습니다."
            );
        }

        return profile;
    }

    @Override
    @Transactional
    public MyProfileResponse updateMyProfile(
            Long userId,
            UpdateMyProfileRequest request
    ) {
        validateUserId(userId);

        String normalizedName = normalizeName(request);

        int updatedRowCount =
                myProfileMapper.updateNameByUserId(
                        userId,
                        normalizedName
                );

        if (updatedRowCount == 0) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "MYP_USER_NOT_FOUND",
                    "회원정보를 찾을 수 없습니다."
            );
        }

        return getMyProfile(userId);
    }

    private String normalizeName(UpdateMyProfileRequest request) {
        if (request == null || request.getName() == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "MYP_NAME_REQUIRED",
                    "이름을 입력해 주세요."
            );
        }

        String normalizedName = request.getName().trim();

        if (normalizedName.isEmpty()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "MYP_NAME_REQUIRED",
                    "이름을 입력해 주세요."
            );
        }

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "MYP_NAME_LENGTH_INVALID",
                    "이름은 100자 이하로 입력해 주세요."
            );
        }

        return normalizedName;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED",
                    "로그인이 필요합니다."
            );
        }
    }
}
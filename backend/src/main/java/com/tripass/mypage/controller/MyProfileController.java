package com.tripass.mypage.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.mypage.dto.MyProfileResponse;
import com.tripass.mypage.dto.UpdateMyProfileRequest;
import com.tripass.mypage.service.MyProfileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "myp-회원정보")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MyProfileController {

    private final MyProfileService myProfileService;

    @ApiOperation(
            value = "내 회원정보 조회",
            notes = "JWT 인증 정보를 기준으로 로그인한 활성 회원의 "
                    + "이름, 로그인 아이디, 휴대전화번호, "
                    + "로그인 제공자 및 가입일시를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> getMyProfile(
            @ApiIgnore Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        MyProfileResponse data =
                myProfileService.getMyProfile(userId);

        return ApiResponse.success(
                "회원정보 조회 성공",
                data
        );
    }

    @ApiOperation(
            value = "내 회원정보 수정",
            notes = "로그인한 회원의 TRIPass 내부 이름을 변경합니다. "
                    + "LOCAL·KAKAO·GOOGLE 회원 모두 변경할 수 있습니다."
    )
    @PatchMapping("/me")
    public ApiResponse<MyProfileResponse> updateMyProfile(
            @ApiIgnore Authentication authentication,

            @ApiParam(
                    value = "변경할 회원정보",
                    required = true
            )
            @RequestBody UpdateMyProfileRequest request
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        MyProfileResponse data =
                myProfileService.updateMyProfile(
                        userId,
                        request
                );

        return ApiResponse.success(
                "회원정보 수정 성공",
                data
        );
    }

    private Long getAuthenticatedUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}
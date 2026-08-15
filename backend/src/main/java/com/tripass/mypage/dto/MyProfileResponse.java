package com.tripass.mypage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
@NoArgsConstructor
@ApiModel(description = "내 회원정보 조회 응답")
public class MyProfileResponse {

    @ApiModelProperty(value = "회원 ID", example = "1")
    private Long id;

    @ApiModelProperty(
            value = "로그인 아이디 또는 소셜 계정 이메일",
            example = "hjsong1102@gmail.com"
    )
    private String loginId;

    @ApiModelProperty(value = "TRIPass에서 사용하는 이름", example = "송형진")
    private String name;

    @ApiModelProperty(
            value = "휴대전화번호",
            example = "01012345678",
            allowEmptyValue = true
    )
    private String phoneNumber;

    @ApiModelProperty(
            value = "로그인 제공자",
            example = "GOOGLE",
            allowableValues = "LOCAL,KAKAO,GOOGLE"
    )
    private String loginProvider;

    @ApiModelProperty(
            value = "가입일시",
            example = "2026-08-15T10:30:00"
    )
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss"
    )
    private LocalDateTime createdAt;
}

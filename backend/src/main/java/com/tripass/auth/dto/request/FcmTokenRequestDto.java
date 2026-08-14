package com.tripass.auth.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(description = "FCM 토큰 등록 요청 DTO")
public class FcmTokenRequestDto {

    @ApiModelProperty(value = "FCM 디바이스 토큰", example = "fcm_token_string_example")
    private String deviceToken;

    @ApiModelProperty(value = "디바이스 유형", allowableValues = "WEB, AOS, IOS", example = "WEB")
    private String deviceType;
}

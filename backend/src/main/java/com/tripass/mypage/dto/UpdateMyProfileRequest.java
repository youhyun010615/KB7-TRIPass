package com.tripass.mypage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@ApiModel(description = "내 회원정보 수정 요청")
public class UpdateMyProfileRequest {

    @ApiModelProperty(
            value = "변경할 TRIPass 내부 이름",
            required = true,
            example = "송형진"
    )
    private String name;
}
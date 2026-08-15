package com.tripass.mypage.mapper;

import com.tripass.mypage.dto.MyProfileResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyProfileMapper {

    // 로그인한 활성 회원의 회원정보 조회
    MyProfileResponse findMyProfileByUserId(
            @Param("userId") Long userId
    );

    // 로그인한 활성 회원의 TRIPass 내부 이름 변경
    int updateNameByUserId(
            @Param("userId") Long userId,
            @Param("name") String name
    );
}
package com.tripass.auth.mapper;

import com.tripass.auth.model.UserFcmToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFcmTokenMapper {
    void saveToken(UserFcmToken token);
    UserFcmToken getTokenByUserId(Long userId);
    void deleteToken(@Param("deviceToken") String deviceToken);
}

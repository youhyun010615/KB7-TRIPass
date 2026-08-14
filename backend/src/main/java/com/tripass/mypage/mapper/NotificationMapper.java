package com.tripass.mypage.mapper;

import com.tripass.mypage.dto.response.NotificationResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    List<NotificationResponseDto> getNotificationsByUserId(Long userId);
    void updateReadStatus(@Param("userId") Long userId, @Param("id") Long id);
    void updateAllReadStatus(@Param("userId") Long userId);
    void insertNotification(@Param("userId") Long userId, @Param("type") String type, @Param("title") String title, @Param("message") String message, @Param("url") String url);
}


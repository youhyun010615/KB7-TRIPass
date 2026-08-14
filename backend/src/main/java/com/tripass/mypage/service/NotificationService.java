package com.tripass.mypage.service;

import com.tripass.mypage.domain.NotificationSetting;
import com.tripass.mypage.dto.response.NotificationResponseDto;
import java.util.List;

public interface NotificationService {
    List<NotificationResponseDto> getNotifications(Long userId);
    void markAsRead(Long userId, Long notificationId);
    void markAllAsRead(Long userId);
    void insertNotification(Long userId, String type, String title, String message, String url);
    
    NotificationSetting getSettings(Long userId);
    void updateSettings(Long userId, NotificationSetting setting);
}

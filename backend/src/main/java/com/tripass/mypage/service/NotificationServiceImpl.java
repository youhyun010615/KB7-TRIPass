package com.tripass.mypage.service;

import com.tripass.mypage.domain.NotificationSetting;
import com.tripass.mypage.dto.response.NotificationResponseDto;
import com.tripass.mypage.mapper.NotificationMapper;
import com.tripass.mypage.mapper.NotificationSettingMapper;
import com.tripass.dev.util.DevDateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationSettingMapper notificationSettingMapper;
    private final DevDateUtil devDateUtil;

    @Override
    public List<NotificationResponseDto> getNotifications(Long userId) {
        return notificationMapper.getNotificationsByUserId(userId, devDateUtil.today(userId));
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        notificationMapper.updateReadStatus(userId, notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMapper.updateAllReadStatus(userId);
    }

    @Override
    @Transactional
    public void insertNotification(Long userId, String type, String title, String message, String url) {
        notificationMapper.insertNotification(userId, type, title, message, url);
    }

    @Override
    public boolean existsNotification(Long userId, String type, String url, String message) {
        return notificationMapper.existsNotification(userId, type, url, message);
    }

    @Override
    public NotificationSetting getSettings(Long userId) {
        return notificationSettingMapper.getSettingByUserId(userId);
    }

    @Override
    @Transactional
    public void updateSettings(Long userId, NotificationSetting setting) {
        setting.setUserId(userId);
        notificationSettingMapper.updateSetting(setting);
    }
}

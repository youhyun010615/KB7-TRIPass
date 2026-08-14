package com.tripass.common.util;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.tripass.auth.mapper.UserFcmTokenMapper;
import com.tripass.auth.model.UserFcmToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserFcmTokenMapper fcmTokenMapper;

    public void sendNotification(Long userId, String title, String message) {
        UserFcmToken fcmToken = fcmTokenMapper.getTokenByUserId(userId);
        if (fcmToken != null) {
            sendNotification(fcmToken.getDeviceToken(), title, message);
        }
    }

    public void sendNotification(String token, String title, String message) {
        Message fcmMessage = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(message)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(fcmMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

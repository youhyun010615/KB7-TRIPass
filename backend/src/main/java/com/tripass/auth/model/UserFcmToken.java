package com.tripass.auth.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFcmToken {
    private Long id;
    private Long userId;
    private String deviceToken;
    private String deviceType; // WEB, AOS, IOS
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

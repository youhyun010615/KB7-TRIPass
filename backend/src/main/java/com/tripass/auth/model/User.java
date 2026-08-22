package com.tripass.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class User {
    private Long id;
    private String loginId;
    private String password;
    private String name;
    private String phoneNumber;
    private String loginProvider;
    private String providerKey;
    private String currentViewMode;
    private boolean deleted;
    private Date deletedAt;
    private Date createdAt;
    private Date updatedAt;
    private LocalDate devOverrideDate;
}

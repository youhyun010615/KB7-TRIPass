package com.tripass.auth.dto.external.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Google OpenID Connect UserInfo 응답
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
public class GoogleUserResponse {

    // Google 계정 고유 식별값
    private String sub;

    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    private String name;

    private String picture;
}
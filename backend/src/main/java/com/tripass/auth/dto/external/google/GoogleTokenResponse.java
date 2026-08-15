package com.tripass.auth.dto.external.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Google Access Token 발급 응답
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
public class GoogleTokenResponse {

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("id_token")
    private String idToken;

    private String scope;
}
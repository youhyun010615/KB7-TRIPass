package com.tripass.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoogleAuthorizationUrlResponse {

    private String authorizationUrl;
}
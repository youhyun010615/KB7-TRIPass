package com.tripass.asset.service.codef;

import com.tripass.common.util.CodefUtil;

import java.util.Map;

final class RealCodefClient implements CodefClient {

    private final String clientId;
    private final String clientSecret;

    RealCodefClient(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String encodePassword(String plainPassword) throws Exception {
        return CodefUtil.encryptRSA(plainPassword);
    }

    @Override
    public String getAccessToken() throws Exception {
        return CodefUtil.getAccessToken(clientId, clientSecret);
    }

    @Override
    public Map<String, Object> callApi(String accessToken, String path, Map<String, Object> body) throws Exception {
        return CodefUtil.callApi(accessToken, path, body);
    }
}

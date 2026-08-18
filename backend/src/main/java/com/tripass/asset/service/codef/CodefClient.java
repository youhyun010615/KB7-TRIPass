package com.tripass.asset.service.codef;

import java.util.Map;

/**
 * CODEF 연동 경계. 서비스 로직은 실제/QA Mock 구현을 구분하지 않는다.
 */
public interface CodefClient {

    String encodePassword(String plainPassword) throws Exception;

    String getAccessToken() throws Exception;

    Map<String, Object> callApi(String accessToken, String path, Map<String, Object> body) throws Exception;
}

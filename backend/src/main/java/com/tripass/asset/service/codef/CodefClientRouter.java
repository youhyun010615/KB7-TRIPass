package com.tripass.asset.service.codef;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * codef.mode 값에 따라 실제 CODEF와 로컬 QA Mock을 전환한다.
 */
@Service
public class CodefClientRouter implements CodefClient {

    private final CodefClient delegate;

    public CodefClientRouter(
            @Value("${codef.mode:real}") String mode,
            @Value("${codef.client-id:}") String clientId,
            @Value("${codef.client-secret:}") String clientSecret
    ) {
        this.delegate = "mock".equalsIgnoreCase(mode)
                ? new MockCodefClient()
                : new RealCodefClient(clientId, clientSecret);
    }

    @Override
    public String encodePassword(String plainPassword) throws Exception {
        return delegate.encodePassword(plainPassword);
    }

    @Override
    public String getAccessToken() throws Exception {
        return delegate.getAccessToken();
    }

    @Override
    public Map<String, Object> callApi(String accessToken, String path, Map<String, Object> body) throws Exception {
        return delegate.callApi(accessToken, path, body);
    }
}

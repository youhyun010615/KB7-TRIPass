package com.tripass.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.crypto.Cipher;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CodefUtil {

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkBRQKFiP1f8XFUwKgI8G3i5EIttN+fIPJE2PKWSG+Im6aeUrzv+0bDs8SUWsO18H7KUUmVH46UN2LtawJtErzuiH0+ADl7OW6AGsp25lx0d9hOJTi1cRIL0jcDo5VmdE6wPdNgbdkZJ/Ee5J4GwseS+VrhwLz/Si72oWJtWYeS0hKl3Lb43BaNuPgOcTKjPUsDlieaFjPgcwrJ5voTxEGJAevjaW7+s9r4GiB9xd5Pmeswi/wuoR59rvpQN8J1YLGKzgmdn67dZoJ8lTN9lQyVHbOQvfIfSBcQOHDBoqJEILfmWoRSmhKBCJJPmwTtEskDFQF6YKDeS6laEvvFWtuwIDAQAB";
    private static final String SANDBOX_URL = "https://sandbox.codef.io";
    private static final String TOKEN_URL = "https://oauth.codef.io/oauth/token";

    public static String encryptRSA(String plainText) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(PUBLIC_KEY);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(Base64.getEncoder().encodeToString(encrypted), "UTF-8");
    }

    public static String getAccessToken(String clientId, String clientSecret) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(TOKEN_URL);

        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );
        post.setHeader("Authorization", "Basic " + credentials);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity("grant_type=client_credentials&scope=read", StandardCharsets.UTF_8));

        CloseableHttpResponse response = client.execute(post);
        String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        client.close();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(json, Map.class);
        return (String) result.get("access_token");
    }

    public static Map<String, Object> callApi(String accessToken, String path, Map<String, Object> body) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(SANDBOX_URL + path);

        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");

        ObjectMapper mapper = new ObjectMapper();
        post.setEntity(new StringEntity(mapper.writeValueAsString(body), StandardCharsets.UTF_8));

        CloseableHttpResponse response = client.execute(post);
        String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        client.close();

        return mapper.readValue(json, Map.class);
    }
}
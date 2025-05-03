package com.example.piplchecker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiChatService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String secretKey;

    public AiChatService(
            @Value("${baidu.api.key}") String apiKey,
            @Value("${baidu.api.secret}") String secretKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public String getAccessToken() {
        String url = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" + apiKey
                + "&client_secret=" + secretKey;
        ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
        return (String) response.getBody().get("access_token");
    }

    public String getAiResponse(String userMessage, String accessToken) {
        String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token="
                + accessToken;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", new Object[] {
                new HashMap<String, String>() {
                    {
                        put("role", "user");
                        put("content", userMessage);
                    }
                }
        });

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return (String) response.getBody().get("result");
    }
}
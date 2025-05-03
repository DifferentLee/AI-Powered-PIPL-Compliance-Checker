package com.example.piplchecker.service;

import com.example.piplchecker.config.BaiduApiConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

@Service
public class BaiduApiService {
    private final BaiduApiConfig baiduApiConfig;
    private final RestTemplate restTemplate;

    public BaiduApiService(BaiduApiConfig baiduApiConfig) {
        this.baiduApiConfig = baiduApiConfig;
        this.restTemplate = new RestTemplate();
    }

    public String getAccessToken() {
        String url = "https://aip.baidubce.com/oauth/2.0/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", baiduApiConfig.getApiKey());
        params.add("client_secret", baiduApiConfig.getSecretKey());

        Map<String, Object> response = restTemplate.postForObject(url, params, Map.class);
        return response != null ? (String) response.get("access_token") : null;
    }

    public int checkComplianceWithPIPL(String text, String accessToken) {
        String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie_speed?access_token="
                + accessToken;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", "请判断以下隐私政策内容是否符合PIPL要求：符合就输出\"yes\"，不符合就输出\"no\"\n\n" + text);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", new Object[] { message });

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            String result = (String) response.get("result");
            return result.contains("yes") ? 1 : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
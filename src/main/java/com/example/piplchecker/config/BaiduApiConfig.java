package com.example.piplchecker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaiduApiConfig {
    @Value("${baidu.api.key}")
    private String apiKey;

    @Value("${baidu.api.secret}")
    private String secretKey;

    public String getApiKey() {
        return apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
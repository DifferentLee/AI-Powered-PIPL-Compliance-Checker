package com.example.piplchecker.controller;

import com.example.piplchecker.service.AiChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Message cannot be empty");
            return ResponseEntity.badRequest().body(errorMap);
        }

        try {
            String accessToken = aiChatService.getAccessToken();
            if (accessToken == null) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("error", "Failed to get access token");
                return ResponseEntity.badRequest().body(errorMap);
            }

            String aiResponse = aiChatService.getAiResponse(userMessage, accessToken);
            Map<String, String> respMap = new HashMap<>();
            respMap.put("response", aiResponse);
            return ResponseEntity.ok(respMap);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to get AI response: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);
        }
    }
}
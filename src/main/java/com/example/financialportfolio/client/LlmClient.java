package com.example.financialportfolio.client;

import com.example.financialportfolio.config.AiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class LlmClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    public LlmClient(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                     AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    public String chat(String prompt) {
        if (!aiProperties.isEnabled()) {
            return "AI 功能未开启，请检查配置。";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一名专业、审慎、清晰表达的投资组合分析助手。"
                + "你只能基于给定数据分析，不承诺收益，不提供绝对买卖指令。");
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        body.put("messages", messages);
        body.put("temperature", 0.4);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                aiProperties.getBaseUrl(),
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        return extractContent(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "AI 返回为空";
        }

        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return "AI 返回格式异常：缺少 choices";
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return "AI 返回格式异常：choice 解析失败";
        }

        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return "AI 返回格式异常：缺少 message";
        }

        Object contentObj = messageMap.get("content");
        return contentObj == null ? "AI 未返回内容" : contentObj.toString();
    }
}
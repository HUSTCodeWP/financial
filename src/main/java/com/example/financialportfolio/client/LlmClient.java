package com.example.financialportfolio.client;

import com.example.financialportfolio.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@Component
public class LlmClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmClient(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                     AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    public String chat(String prompt) {
        if (!aiProperties.isEnabled()) {
            return "AI is disabled. Please check configuration.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a professional and careful portfolio analysis assistant. " +
                "Follow the language instruction in the user prompt. Do not guarantee profits or provide absolute buy/sell commands.");
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

    public void streamChat(String prompt, Consumer<String> onChunk) throws Exception {
        if (!aiProperties.isEnabled()) {
            onChunk.accept("AI is disabled. Please check configuration.");
            return;
        }

        URL url = new URL(aiProperties.getBaseUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + aiProperties.getApiKey());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(aiProperties.getConnectTimeout());
        connection.setReadTimeout(aiProperties.getReadTimeout());

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("stream", true);
        body.put("temperature", 0.4);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a professional and careful portfolio analysis assistant. " +
                "Follow the language instruction in the user prompt. Do not guarantee profits or provide absolute buy/sell commands.");
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        body.put("messages", messages);

        String requestJson = objectMapper.writeValueAsString(body);
        connection.getOutputStream().write(requestJson.getBytes(StandardCharsets.UTF_8));

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)
            );
            StringBuilder errorText = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorText.append(line);
            }
            throw new RuntimeException("AI stream request failed: HTTP " + status + " - " + errorText);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }

                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }

                JsonNode delta = choices.get(0).path("delta");
                JsonNode contentNode = delta.path("content");
                if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                    onChunk.accept(contentNode.asText());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "AI returned empty content.";
        }

        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return "AI response format error: missing choices";
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return "AI response format error: invalid choice";
        }

        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return "AI response format error: missing message";
        }

        Object contentObj = messageMap.get("content");
        return contentObj == null ? "AI returned no content." : contentObj.toString();
    }
}
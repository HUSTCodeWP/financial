package com.example.financialportfolio.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiConsultantService {

    String chat(String question);

    SseEmitter generatePortfolioAdvice(Long portfolioId);

    String answerPortfolioQuestion(Long portfolioId, String question, String riskPreference);
}
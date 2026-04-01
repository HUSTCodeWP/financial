package com.example.financialportfolio.service;

import com.example.financialportfolio.dto.PortfolioAdviceResponse;

public interface AiConsultantService {

    String chat(String question);

    PortfolioAdviceResponse generatePortfolioAdvice(Long portfolioId);

    String answerPortfolioQuestion(Long portfolioId, String question, String riskPreference);
}
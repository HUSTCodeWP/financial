package com.example.financialportfolio.dto;

public class PortfolioQuestionResponse {

    private String answer;

    public PortfolioQuestionResponse() {
    }

    public PortfolioQuestionResponse(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
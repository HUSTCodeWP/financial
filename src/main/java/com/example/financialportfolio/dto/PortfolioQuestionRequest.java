package com.example.financialportfolio.dto;

public class PortfolioQuestionRequest {

    private String question;
    private String riskPreference;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getRiskPreference() {
        return riskPreference;
    }

    public void setRiskPreference(String riskPreference) {
        this.riskPreference = riskPreference;
    }
}
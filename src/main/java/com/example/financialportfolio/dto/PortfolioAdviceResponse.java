package com.example.financialportfolio.dto;

import java.util.List;

public class PortfolioAdviceResponse {

    private String summary;
    private String riskLevel;
    private List<String> riskPoints;
    private List<String> suggestions;
    private String disclaimer;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getRiskPoints() {
        return riskPoints;
    }

    public void setRiskPoints(List<String> riskPoints) {
        this.riskPoints = riskPoints;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
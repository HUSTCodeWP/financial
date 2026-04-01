package com.example.financialportfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioAnalysisContext {

    private Long portfolioId;
    private String portfolioName;
    private BigDecimal totalMarketValue;
    private BigDecimal totalCostValue;
    private BigDecimal totalProfit;
    private BigDecimal totalReturnRate;
    private BigDecimal maxHoldingWeight;
    private Integer holdingCount;
    private List<HoldingAnalysisItem> holdings;

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public BigDecimal getTotalMarketValue() {
        return totalMarketValue;
    }

    public void setTotalMarketValue(BigDecimal totalMarketValue) {
        this.totalMarketValue = totalMarketValue;
    }

    public BigDecimal getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(BigDecimal totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getTotalReturnRate() {
        return totalReturnRate;
    }

    public void setTotalReturnRate(BigDecimal totalReturnRate) {
        this.totalReturnRate = totalReturnRate;
    }

    public BigDecimal getMaxHoldingWeight() {
        return maxHoldingWeight;
    }

    public void setMaxHoldingWeight(BigDecimal maxHoldingWeight) {
        this.maxHoldingWeight = maxHoldingWeight;
    }

    public Integer getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(Integer holdingCount) {
        this.holdingCount = holdingCount;
    }

    public List<HoldingAnalysisItem> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<HoldingAnalysisItem> holdings) {
        this.holdings = holdings;
    }
}
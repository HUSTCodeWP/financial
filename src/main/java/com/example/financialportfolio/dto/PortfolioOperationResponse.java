package com.example.financialportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PortfolioOperationResponse {

    private Long portfolioId;
    private BigDecimal reward;
    private BigDecimal risk;
}
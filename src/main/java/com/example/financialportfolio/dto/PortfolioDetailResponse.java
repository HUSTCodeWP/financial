package com.example.financialportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class PortfolioDetailResponse {

    private Long id;
    private String portfolioName;
    private List<PortfolioDetailItemDto> details;
    private BigDecimal expectedReturn;
    private BigDecimal expectedVolatility;
}
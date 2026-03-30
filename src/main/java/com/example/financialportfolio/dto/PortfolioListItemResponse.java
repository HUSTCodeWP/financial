package com.example.financialportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PortfolioListItemResponse {

    private Long id;
    private String portfolioName;
}
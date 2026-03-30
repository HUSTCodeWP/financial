package com.example.financialportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDetailItemDto {

    private String stockCode;
    private BigDecimal ratio;
}
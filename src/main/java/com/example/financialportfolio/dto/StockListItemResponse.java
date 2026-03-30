package com.example.financialportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class StockListItemResponse {

    private String stockCode;
    private String chineseName;
    private BigDecimal close;
}
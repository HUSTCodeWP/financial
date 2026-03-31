package com.example.financialportfolio.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockListItemResponse {
    private String code;
    private String name;
    private String market;
    private BigDecimal latestPrice; // 新增：最新收盘价
}
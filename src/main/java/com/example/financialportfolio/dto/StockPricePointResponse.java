package com.example.financialportfolio.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockPricePointResponse {
    private String stockCode;
    private LocalDateTime ts; // 统一为 LocalDateTime
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
}
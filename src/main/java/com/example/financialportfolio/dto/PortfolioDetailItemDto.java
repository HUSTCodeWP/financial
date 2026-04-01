package com.example.financialportfolio.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotBlank(message = "Stock code must not be blank")
    private String stockCode;

    @NotNull(message = "Ratio must not be null")
    @DecimalMin(value = "0.0000", inclusive = true, message = "Ratio must be greater than or equal to 0")
    @DecimalMax(value = "1.0000", inclusive = true, message = "Ratio must be less than or equal to 1")
    private BigDecimal ratio;

    @NotNull(message = "Shares must not be null")
    @Positive(message = "Shares must be greater than 0")
    private Integer shares;

    // create 时允许前端不传；update 时由 service 自己校验
    private BigDecimal closePrice;
}
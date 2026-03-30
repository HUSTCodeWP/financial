package com.example.financialportfolio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "portfolio_item")
@Getter
@Setter
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratio;
}
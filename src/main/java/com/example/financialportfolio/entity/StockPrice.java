package com.example.financialportfolio.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_price",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_time", columnNames = {"stock_code", "ts"})
        }
)

@Data
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_code", referencedColumnName = "stock_code", nullable = false)
    private Stock stock;

    @Column(name = "ts", nullable = false)
    private LocalDateTime ts;

    @Column(name = "open", nullable = false, precision = 10, scale = 2)
    private BigDecimal open;

    @Column(name = "high", nullable = false, precision = 10, scale = 2)
    private BigDecimal high;

    @Column(name = "low", nullable = false, precision = 10, scale = 2)
    private BigDecimal low;

    @Column(name = "close", nullable = false, precision = 10, scale = 2)
    private BigDecimal close;

    @Column(name = "volume", nullable = false)
    private Long volume;
}
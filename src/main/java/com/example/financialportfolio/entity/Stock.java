package com.example.financialportfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock")
@Getter
@Setter
public class Stock {

    @Id
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

    @Column(name = "chinese_name", length = 100, nullable = false)
    private String chineseName;

    @Column(name = "english_name", length = 200)
    private String englishName;

    @Column(name = "market", length = 20)
    private String market;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
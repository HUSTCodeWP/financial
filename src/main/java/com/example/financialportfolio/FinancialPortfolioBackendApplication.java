package com.example.financialportfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialPortfolioBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialPortfolioBackendApplication.class, args);
    }
}
package com.example.financialportfolio.repository;

import com.example.financialportfolio.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {

    List<Stock> findByIsActiveTrueOrderByStockCodeAsc();
}
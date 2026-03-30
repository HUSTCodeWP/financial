package com.example.financialportfolio.repository;

import com.example.financialportfolio.entity.Stock;
import com.example.financialportfolio.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    List<StockPrice> findByStockOrderByTsAsc(Stock stock);

    Optional<StockPrice> findTopByStockOrderByTsDesc(Stock stock);
}
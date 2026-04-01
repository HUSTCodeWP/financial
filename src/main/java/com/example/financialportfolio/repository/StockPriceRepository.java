package com.example.financialportfolio.repository;

import com.example.financialportfolio.entity.Stock;
import com.example.financialportfolio.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    @Query("SELECT sp.ts FROM StockPrice sp WHERE sp.stock = :stock AND sp.ts IN :tsList")
    List<LocalDateTime> findExistingTsByStockAndTsIn(
            @Param("stock") Stock stock,
            @Param("tsList") List<LocalDateTime> tsList
    );

    List<StockPrice> findByStockOrderByTsAsc(Stock stock);

    List<StockPrice> findByStockAndTsBetweenOrderByTsAsc(
            Stock stock,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<StockPrice> findTopByStockOrderByTsDesc(Stock stock);

    Optional<StockPrice> findTopByStockAndTsLessThanEqualOrderByTsDesc(Stock stock, LocalDateTime ts);

    boolean existsByStockAndTs(Stock stock, LocalDateTime ts);
}
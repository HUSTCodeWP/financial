package com.example.financialportfolio.service;

import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface StockService {

    List<StockListItemResponse> getAllStocks();

    List<StockPricePointResponse> getStockPrices(String stockCode);

    void saveStockPrice(StockPricePointResponse response);

    void updateStockInfo(StockListItemResponse response);

    void backfillMissingPrices(String stockCode, LocalDateTime startTime, LocalDateTime endTime);
}
package com.example.financialportfolio.service;

import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;

import java.util.List;

public interface StockService {

    List<StockListItemResponse> getAllStocks();

    List<StockPricePointResponse> getStockPrices(String stockCode);
}
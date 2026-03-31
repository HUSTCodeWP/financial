package com.example.financialportfolio.service;

import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;

import java.util.List;

public interface StockService {

    List<StockListItemResponse> getAllStocks();

    List<StockPricePointResponse> getStockPrices(String stockCode);
    // 新增：保存实时行情
    void saveStockPrice(StockPricePointResponse response);

    // 新增：更新股票基础信息
    void updateStockInfo(StockListItemResponse response);
}
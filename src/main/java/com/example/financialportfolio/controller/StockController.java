package com.example.financialportfolio.controller;

import com.example.financialportfolio.common.result.ApiResponse;
import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;
import com.example.financialportfolio.service.StockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // 新路径
    @GetMapping
    public ApiResponse<List<StockListItemResponse>> getAllStocks() {
        return ApiResponse.success(stockService.getAllStocks(), "return successful");
    }

    // 兼容旧路径
    @GetMapping("/getAllStockInfo")
    public ApiResponse<List<StockListItemResponse>> getAllStockInfo() {
        return ApiResponse.success(stockService.getAllStocks(), "return successful");
    }

    // 新路径
    @GetMapping("/{stockCode}/prices")
    public ApiResponse<List<StockPricePointResponse>> getStockPrices(@PathVariable String stockCode) {
        return ApiResponse.success(stockService.getStockPrices(stockCode), "stock data fetched successfully");
    }

    // 兼容旧路径
    @GetMapping("/getStockInfoList/{stockCode}")
    public ApiResponse<List<StockPricePointResponse>> getStockInfoList(@PathVariable String stockCode) {
        return ApiResponse.success(stockService.getStockPrices(stockCode), "stock data fetched successfully");
    }
}
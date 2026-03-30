package com.example.financialportfolio.service.impl;

import com.example.financialportfolio.common.exception.ResourceNotFoundException;
import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;
import com.example.financialportfolio.entity.Stock;
import com.example.financialportfolio.entity.StockPrice;
import com.example.financialportfolio.repository.StockPriceRepository;
import com.example.financialportfolio.repository.StockRepository;
import com.example.financialportfolio.service.StockService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    public StockServiceImpl(StockRepository stockRepository,
                            StockPriceRepository stockPriceRepository) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    @Override
    public List<StockListItemResponse> getAllStocks() {
        List<Stock> stocks = stockRepository.findByIsActiveTrueOrderByStockCodeAsc();
        List<StockListItemResponse> result = new ArrayList<>();

        for (Stock stock : stocks) {
            StockPrice latestPrice = stockPriceRepository.findTopByStockOrderByTsDesc(stock)
                    .orElse(null);

            result.add(new StockListItemResponse(
                    stock.getStockCode(),
                    stock.getChineseName(),
                    latestPrice != null ? latestPrice.getClose() : null
            ));
        }

        return result;
    }

    @Override
    public List<StockPricePointResponse> getStockPrices(String stockCode) {
        Stock stock = stockRepository.findById(stockCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + stockCode));

        List<StockPrice> prices = stockPriceRepository.findByStockOrderByTsAsc(stock);

        if (prices.isEmpty()) {
            throw new ResourceNotFoundException("No price data found for stock: " + stockCode);
        }

        List<StockPricePointResponse> result = new ArrayList<>();
        for (StockPrice price : prices) {
            result.add(new StockPricePointResponse(
                    price.getTs(),
                    price.getOpen(),
                    price.getHigh(),
                    price.getLow(),
                    price.getClose(),
                    price.getVolume()
            ));
        }

        return result;
    }
}
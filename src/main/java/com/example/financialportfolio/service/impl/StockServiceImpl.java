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

import java.time.LocalDateTime;
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

            StockListItemResponse response = new StockListItemResponse();
            response.setCode(stock.getStockCode());
            response.setName(stock.getChineseName());
            response.setMarket(stock.getMarket());
            response.setLatestPrice(latestPrice != null ? latestPrice.getClose() : null);
            result.add(response);
        }

        return result;
    }

    @Override
    public List<StockPricePointResponse> getStockPrices(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + stockCode));

        List<StockPrice> prices = stockPriceRepository.findByStockOrderByTsAsc(stock);

        if (prices.isEmpty()) {
            throw new ResourceNotFoundException("No price data found for stock: " + stockCode);
        }

        List<StockPricePointResponse> result = new ArrayList<>();
        for (StockPrice price : prices) {
            StockPricePointResponse resp = new StockPricePointResponse();
            resp.setStockCode(price.getStock().getStockCode());
            resp.setTs(price.getTs());
            resp.setOpen(price.getOpen());
            resp.setHigh(price.getHigh());
            resp.setLow(price.getLow());
            resp.setClose(price.getClose());
            resp.setVolume(price.getVolume());
            result.add(resp);
        }

        return result;
    }

    @Override
    public void saveStockPrice(StockPricePointResponse response) {
        Stock stock = stockRepository.findByStockCode(response.getStockCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock not found: " + response.getStockCode()
                ));

        StockPrice price = new StockPrice();
        price.setStock(stock);
        price.setTs(response.getTs() != null ? response.getTs() : LocalDateTime.now());
        price.setOpen(response.getOpen());
        price.setHigh(response.getHigh());
        price.setLow(response.getLow());
        price.setClose(response.getClose());
        price.setVolume(response.getVolume());

        stockPriceRepository.save(price);
    }

    @Override
    public void updateStockInfo(StockListItemResponse response) {
        Stock stock = stockRepository.findByStockCode(response.getCode())
                .orElse(new Stock());

        stock.setStockCode(response.getCode());
        stock.setChineseName(response.getName());
        stock.setMarket(response.getMarket());
        stock.setIsActive(true);

        stockRepository.save(stock);
    }
}
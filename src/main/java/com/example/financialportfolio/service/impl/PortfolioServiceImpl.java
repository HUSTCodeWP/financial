package com.example.financialportfolio.service.impl;

import com.example.financialportfolio.common.exception.ResourceNotFoundException;
import com.example.financialportfolio.dto.*;
import com.example.financialportfolio.entity.Portfolio;
import com.example.financialportfolio.entity.PortfolioItem;
import com.example.financialportfolio.repository.PortfolioRepository;
import com.example.financialportfolio.repository.StockRepository;
import com.example.financialportfolio.service.PortfolioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository,
                                StockRepository stockRepository) {
        this.portfolioRepository = portfolioRepository;
        this.stockRepository = stockRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioListItemResponse> getAllPortfolios() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        List<PortfolioListItemResponse> result = new ArrayList<>();

        for (Portfolio portfolio : portfolios) {
            result.add(new PortfolioListItemResponse(
                    portfolio.getId(),
                    portfolio.getPortfolioName()
            ));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioDetailResponse getPortfolioById(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + id));

        List<PortfolioDetailItemDto> details = new ArrayList<>();
        for (PortfolioItem item : portfolio.getItems()) {
            details.add(new PortfolioDetailItemDto(
                    item.getStockCode(),
                    item.getRatio()
            ));
        }

        return new PortfolioDetailResponse(
                portfolio.getId(),
                portfolio.getPortfolioName(),
                details,
                portfolio.getExpectedReturn(),
                portfolio.getExpectedVolatility()
        );
    }

    @Override
    public PortfolioOperationResponse createPortfolio(CreatePortfolioRequest request) {
        validatePortfolioRequest(request.getName(), request.getDetails());

        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioName(request.getName().trim());

        BigDecimal reward = BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        BigDecimal risk = BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);

        portfolio.setExpectedReturn(reward);
        portfolio.setExpectedVolatility(risk);

        List<PortfolioItem> items = buildPortfolioItems(portfolio, request.getDetails());
        portfolio.setItems(items);

        Portfolio saved = portfolioRepository.save(portfolio);

        return new PortfolioOperationResponse(saved.getId(), reward, risk);
    }

    @Override
    public PortfolioOperationResponse updatePortfolio(Long id, UpdatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + id));

        validatePortfolioRequest(request.getName(), request.getDetails());

        portfolio.setPortfolioName(request.getName().trim());

        BigDecimal reward = BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        BigDecimal risk = BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);

        portfolio.setExpectedReturn(reward);
        portfolio.setExpectedVolatility(risk);

        portfolio.getItems().clear();
        portfolio.getItems().addAll(buildPortfolioItems(portfolio, request.getDetails()));

        Portfolio updated = portfolioRepository.save(portfolio);

        return new PortfolioOperationResponse(updated.getId(), reward, risk);
    }

    @Override
    public void deletePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + id));

        portfolioRepository.delete(portfolio);
    }

    private void validatePortfolioRequest(String name, List<PortfolioDetailItemDto> details) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Portfolio name must not be blank");
        }

        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Portfolio details must not be empty");
        }

        BigDecimal totalRatio = BigDecimal.ZERO;

        for (int i = 0; i < details.size(); i++) {
            PortfolioDetailItemDto item = details.get(i);

            if (item.getStockCode() == null || item.getStockCode().isBlank()) {
                throw new IllegalArgumentException("Stock code must not be blank at index " + i);
            }

            if (item.getRatio() == null || item.getRatio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Ratio must be greater than 0 at index " + i);
            }

            if (!stockRepository.existsById(item.getStockCode())) {
                throw new ResourceNotFoundException("Stock not found: " + item.getStockCode());
            }

            totalRatio = totalRatio.add(item.getRatio());
        }

        BigDecimal diff = totalRatio.subtract(BigDecimal.ONE).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("The sum of stock allocation ratios must equal 1");
        }
    }

    private List<PortfolioItem> buildPortfolioItems(Portfolio portfolio, List<PortfolioDetailItemDto> details) {
        List<PortfolioItem> items = new ArrayList<>();

        for (PortfolioDetailItemDto detail : details) {
            PortfolioItem item = new PortfolioItem();
            item.setPortfolio(portfolio);
            item.setStockCode(detail.getStockCode());
            item.setRatio(detail.getRatio());
            items.add(item);
        }

        return items;
    }
}
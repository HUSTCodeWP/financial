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
                .orElseThrow(() -> new ResourceNotFoundException("组合不存在，ID：" + id)); // 优化提示语

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

        // 预留：此处应根据股票权重计算预期收益/波动率，示例先保留逻辑，后续替换
        BigDecimal expectedReturn = calculateExpectedReturn(request.getDetails());
        BigDecimal expectedVolatility = calculateExpectedVolatility(request.getDetails());

        portfolio.setExpectedReturn(expectedReturn);
        portfolio.setExpectedVolatility(expectedVolatility);

        List<PortfolioItem> items = buildPortfolioItems(portfolio, request.getDetails());
        portfolio.setItems(items);

        Portfolio saved = portfolioRepository.save(portfolio);

        return new PortfolioOperationResponse(saved.getId(), expectedReturn, expectedVolatility);
    }

    @Override
    public PortfolioOperationResponse updatePortfolio(Long id, UpdatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组合不存在，ID：" + id)); // 优化提示语

        validatePortfolioRequest(request.getName(), request.getDetails());

        portfolio.setPortfolioName(request.getName().trim());

        // 预留：计算预期收益/波动率
        BigDecimal expectedReturn = calculateExpectedReturn(request.getDetails());
        BigDecimal expectedVolatility = calculateExpectedVolatility(request.getDetails());

        portfolio.setExpectedReturn(expectedReturn);
        portfolio.setExpectedVolatility(expectedVolatility);

        portfolio.getItems().clear();
        portfolio.getItems().addAll(buildPortfolioItems(portfolio, request.getDetails()));

        Portfolio updated = portfolioRepository.save(portfolio);

        return new PortfolioOperationResponse(updated.getId(), expectedReturn, expectedVolatility);
    }

    @Override
    public void deletePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组合不存在，ID：" + id)); // 优化提示语

        portfolioRepository.delete(portfolio);
    }

    private void validatePortfolioRequest(String name, List<PortfolioDetailItemDto> details) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("组合名称不能为空"); // 优化中文提示
        }

        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("组合持仓明细不能为空"); // 优化中文提示
        }

        BigDecimal totalRatio = BigDecimal.ZERO;

        for (int i = 0; i < details.size(); i++) {
            PortfolioDetailItemDto item = details.get(i);

            if (item.getStockCode() == null || item.getStockCode().isBlank()) {
                throw new IllegalArgumentException("第" + (i+1) + "条持仓的股票代码不能为空"); // 优化索引（从1开始）
            }

            if (item.getRatio() == null || item.getRatio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("第" + (i+1) + "条持仓的权重必须大于0"); // 优化索引
            }

            if (!stockRepository.existsById(item.getStockCode())) {
                throw new ResourceNotFoundException("股票不存在：" + item.getStockCode()); // 优化提示语
            }

            totalRatio = totalRatio.add(item.getRatio()).setScale(5, RoundingMode.HALF_UP);
        }

        // 优化提示语：明确误差范围
        BigDecimal diff = totalRatio.subtract(BigDecimal.ONE).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("持仓权重总和需等于1（允许±1%误差），当前总和：" + totalRatio);
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

    // 预留：计算组合预期收益（示例逻辑，需根据实际业务调整）
    private BigDecimal calculateExpectedReturn(List<PortfolioDetailItemDto> details) {
        BigDecimal totalReturn = BigDecimal.ZERO;
        // 示例：假设从StockRepository获取单只股票的预期收益，再按权重累加
        for (PortfolioDetailItemDto item : details) {
            // 实际业务中需补充：Stock stock = stockRepository.findById(item.getStockCode()).get();
            // BigDecimal stockReturn = stock.getExpectedReturn();
            // totalReturn = totalReturn.add(stockReturn.multiply(item.getRatio()));
        }
        return totalReturn.setScale(5, RoundingMode.HALF_UP);
    }

    // 预留：计算组合预期波动率（示例逻辑，需根据实际业务调整）
    private BigDecimal calculateExpectedVolatility(List<PortfolioDetailItemDto> details) {
        BigDecimal totalVolatility = BigDecimal.ZERO;
        // 实际业务中需补充波动率计算逻辑（如协方差矩阵）
        return totalVolatility.setScale(5, RoundingMode.HALF_UP);
    }
}
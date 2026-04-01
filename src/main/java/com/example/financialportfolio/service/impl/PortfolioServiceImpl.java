package com.example.financialportfolio.service.impl;

import com.example.financialportfolio.common.exception.ResourceNotFoundException;
import com.example.financialportfolio.dto.CreatePortfolioRequest;
import com.example.financialportfolio.dto.PortfolioDetailItemDto;
import com.example.financialportfolio.dto.PortfolioDetailResponse;
import com.example.financialportfolio.dto.PortfolioListItemResponse;
import com.example.financialportfolio.dto.PortfolioOperationResponse;
import com.example.financialportfolio.dto.UpdatePortfolioRequest;
import com.example.financialportfolio.entity.Portfolio;
import com.example.financialportfolio.entity.PortfolioItem;
import com.example.financialportfolio.repository.PortfolioItemRepository;
import com.example.financialportfolio.repository.PortfolioRepository;
import com.example.financialportfolio.repository.StockRepository;
import com.example.financialportfolio.service.PortfolioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final StockRepository stockRepository;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository,
                                PortfolioItemRepository portfolioItemRepository,
                                StockRepository stockRepository) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioItemRepository = portfolioItemRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("组合不存在，ID：" + id));

        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioId(id);
        List<PortfolioDetailItemDto> details = new ArrayList<>();

        for (PortfolioItem item : items) {
            details.add(new PortfolioDetailItemDto(
                    item.getStockCode(),
                    item.getCurrentRatio() != null ? item.getCurrentRatio() : item.getRatio(),
                    item.getCurrentShares() != null ? item.getCurrentShares() : item.getShares(),
                    item.getCurrentPrice() != null ? item.getCurrentPrice() : item.getClosePrice()
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

        // 当前版本不在后端计算收益率和波动率
        portfolio.setExpectedReturn(null);
        portfolio.setExpectedVolatility(null);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        List<PortfolioItem> items = buildPortfolioItems(savedPortfolio, request.getDetails());
        portfolioItemRepository.saveAll(items);

        return new PortfolioOperationResponse(
                savedPortfolio.getId(),
                savedPortfolio.getExpectedReturn(),
                savedPortfolio.getExpectedVolatility()
        );
    }

    @Override
    public PortfolioOperationResponse updatePortfolio(Long id, UpdatePortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组合不存在，ID：" + id));

        validatePortfolioRequest(request.getName(), request.getDetails());

        portfolio.setPortfolioName(request.getName().trim());
        portfolio.setExpectedReturn(null);
        portfolio.setExpectedVolatility(null);
        portfolioRepository.save(portfolio);

        List<PortfolioItem> existingItems = portfolioItemRepository.findByPortfolioId(id);

        Map<String, PortfolioItem> existingItemMap = new HashMap<>();
        for (PortfolioItem item : existingItems) {
            existingItemMap.put(item.getStockCode(), item);
        }

        Set<String> requestStockCodes = new HashSet<>();

        for (PortfolioDetailItemDto detail : request.getDetails()) {
            requestStockCodes.add(detail.getStockCode());

            PortfolioItem existingItem = existingItemMap.get(detail.getStockCode());

            if (existingItem != null) {
                // 只更新当前字段，不覆盖 create 时原始数据
                existingItem.setCurrentRatio(detail.getRatio());
                existingItem.setCurrentShares(detail.getShares());
                existingItem.setCurrentPrice(detail.getClosePrice());
            } else {
                // 新增股票：原始字段和当前字段都初始化为本次值
                PortfolioItem newItem = new PortfolioItem();
                newItem.setPortfolio(portfolio);
                newItem.setStockCode(detail.getStockCode());

                // create 原始字段
                newItem.setRatio(detail.getRatio());
                newItem.setShares(detail.getShares());
                newItem.setClosePrice(detail.getClosePrice());

                // current 当前字段
                newItem.setCurrentRatio(detail.getRatio());
                newItem.setCurrentShares(detail.getShares());
                newItem.setCurrentPrice(detail.getClosePrice());

                existingItems.add(newItem);
            }
        }

        // 对于前端本次未传入的旧股票：保留原始数据，但当前持仓置零
        for (PortfolioItem item : existingItems) {
            if (!requestStockCodes.contains(item.getStockCode())) {
                item.setCurrentRatio(BigDecimal.ZERO);
                item.setCurrentShares(0);
                item.setCurrentPrice(BigDecimal.ZERO);
            }
        }

        portfolioItemRepository.saveAll(existingItems);

        return new PortfolioOperationResponse(
                portfolio.getId(),
                portfolio.getExpectedReturn(),
                portfolio.getExpectedVolatility()
        );
    }

    @Override
    public void deletePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组合不存在，ID：" + id));

        portfolioItemRepository.deleteByPortfolioId(id);
        portfolioRepository.delete(portfolio);
    }

    private void validatePortfolioRequest(String name, List<PortfolioDetailItemDto> details) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("组合名称不能为空");
        }

        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("组合持仓明细不能为空");
        }

        for (int i = 0; i < details.size(); i++) {
            PortfolioDetailItemDto item = details.get(i);

            if (item.getStockCode() == null || item.getStockCode().isBlank()) {
                throw new IllegalArgumentException("第" + (i + 1) + "条持仓的股票代码不能为空");
            }

            if (item.getRatio() == null || item.getRatio().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("第" + (i + 1) + "条持仓的权重不能小于0");
            }

            if (item.getShares() == null || item.getShares() <= 0) {
                throw new IllegalArgumentException("第" + (i + 1) + "条持仓的股数必须大于0");
            }

            if (item.getClosePrice() == null || item.getClosePrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("第" + (i + 1) + "条持仓的收盘价必须大于0");
            }

            if (!stockRepository.existsById(item.getStockCode())) {
                throw new ResourceNotFoundException("股票不存在：" + item.getStockCode());
            }
        }
    }

    private List<PortfolioItem> buildPortfolioItems(Portfolio portfolio, List<PortfolioDetailItemDto> details) {
        List<PortfolioItem> items = new ArrayList<>();

        for (PortfolioDetailItemDto detail : details) {
            PortfolioItem item = new PortfolioItem();
            item.setPortfolio(portfolio);
            item.setStockCode(detail.getStockCode());

            // create 原始字段
            item.setRatio(detail.getRatio());
            item.setShares(detail.getShares());
            item.setClosePrice(detail.getClosePrice());

            // current 当前字段（初始化时和 create 一致）
            item.setCurrentRatio(detail.getRatio());
            item.setCurrentShares(detail.getShares());
            item.setCurrentPrice(detail.getClosePrice());

            items.add(item);
        }

        return items;
    }
}
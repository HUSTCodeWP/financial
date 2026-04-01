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
import java.util.List;

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
                    item.getRatio(),
                    item.getShares(),
                    item.getClosePrice()
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

        // 这次需求中不再由后端计算，先置空
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

        // 这次需求中不再由后端计算，更新时也置空
        portfolio.setExpectedReturn(null);
        portfolio.setExpectedVolatility(null);

        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);

        portfolioItemRepository.deleteByPortfolioId(id);

        List<PortfolioItem> newItems = buildPortfolioItems(updatedPortfolio, request.getDetails());
        portfolioItemRepository.saveAll(newItems);

        return new PortfolioOperationResponse(
                updatedPortfolio.getId(),
                updatedPortfolio.getExpectedReturn(),
                updatedPortfolio.getExpectedVolatility()
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
            item.setRatio(detail.getRatio());
            item.setShares(detail.getShares());
            item.setClosePrice(detail.getClosePrice());
            items.add(item);
        }

        return items;
    }
}
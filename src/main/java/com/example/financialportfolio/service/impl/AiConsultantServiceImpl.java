package com.example.financialportfolio.service.impl;

import com.example.financialportfolio.client.LlmClient;
import com.example.financialportfolio.dto.HoldingAnalysisItem;
import com.example.financialportfolio.dto.PortfolioAdviceResponse;
import com.example.financialportfolio.dto.PortfolioAnalysisContext;
import com.example.financialportfolio.entity.Portfolio;
import com.example.financialportfolio.entity.PortfolioItem;
import com.example.financialportfolio.entity.Stock;
import com.example.financialportfolio.prompt.PromptBuilder;
import com.example.financialportfolio.repository.PortfolioItemRepository;
import com.example.financialportfolio.repository.PortfolioRepository;
import com.example.financialportfolio.repository.StockRepository;
import com.example.financialportfolio.service.AiConsultantService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AiConsultantServiceImpl implements AiConsultantService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final StockRepository stockRepository;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;

    public AiConsultantServiceImpl(PortfolioRepository portfolioRepository,
                                   PortfolioItemRepository portfolioItemRepository,
                                   StockRepository stockRepository,
                                   LlmClient llmClient,
                                   PromptBuilder promptBuilder) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.stockRepository = stockRepository;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String chat(String question) {
        String prompt = promptBuilder.buildGeneralQuestionPrompt(question);
        return llmClient.chat(prompt);
    }

    @Override
    public PortfolioAdviceResponse generatePortfolioAdvice(Long portfolioId) {
        PortfolioAnalysisContext context = buildPortfolioContext(portfolioId);
        String prompt = promptBuilder.buildPortfolioAdvicePrompt(context);
        String answer = llmClient.chat(prompt);

        PortfolioAdviceResponse response = new PortfolioAdviceResponse();
        response.setSummary(answer);
        response.setRiskLevel(calculateRiskLevel(context));
        response.setRiskPoints(buildRiskPoints(context));
        response.setSuggestions(buildBasicSuggestions(context));
        response.setDisclaimer("以上内容仅供参考，不构成任何投资承诺或买卖建议，投资需结合自身风险承受能力审慎决策。");
        return response;
    }

    @Override
    public String answerPortfolioQuestion(Long portfolioId, String question, String riskPreference) {
        PortfolioAnalysisContext context = buildPortfolioContext(portfolioId);
        String prompt = promptBuilder.buildPortfolioQuestionPrompt(context, question, riskPreference);
        return llmClient.chat(prompt);
    }

    private PortfolioAnalysisContext buildPortfolioContext(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("投资组合不存在: " + portfolioId));

        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioId(portfolioId);

        List<HoldingAnalysisItem> holdingAnalysisItems = new ArrayList<>();
        BigDecimal maxWeight = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            String stockCode = item.getStockCode();
            BigDecimal weight = safe(item.getRatio());

            String stockName = stockCode;
            Optional<Stock> stockOpt = stockRepository.findByStockCode(stockCode);
            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                stockName = stock.getChineseName();
            }

            HoldingAnalysisItem analysisItem = new HoldingAnalysisItem();
            analysisItem.setStockCode(stockCode);
            analysisItem.setStockName(stockName);
            analysisItem.setWeight(weight);

            // 当前版本不支持数量/买入价/收益分析，统一置 0
            analysisItem.setQuantity(BigDecimal.ZERO);
            analysisItem.setCostPrice(BigDecimal.ZERO);
            analysisItem.setLatestPrice(BigDecimal.ZERO);
            analysisItem.setMarketValue(BigDecimal.ZERO);
            analysisItem.setCostValue(BigDecimal.ZERO);
            analysisItem.setProfitAmount(BigDecimal.ZERO);
            analysisItem.setReturnRate(BigDecimal.ZERO);

            holdingAnalysisItems.add(analysisItem);

            if (weight.compareTo(maxWeight) > 0) {
                maxWeight = weight;
            }
        }

        PortfolioAnalysisContext context = new PortfolioAnalysisContext();
        context.setPortfolioId(portfolioId);
        context.setPortfolioName(portfolio.getPortfolioName());

        // 当前版本不支持收益相关计算，统一置 0
        context.setTotalMarketValue(BigDecimal.ZERO);
        context.setTotalCostValue(BigDecimal.ZERO);
        context.setTotalProfit(BigDecimal.ZERO);
        context.setTotalReturnRate(BigDecimal.ZERO);

        context.setMaxHoldingWeight(maxWeight);
        context.setHoldingCount(holdingAnalysisItems.size());
        context.setHoldings(holdingAnalysisItems);

        return context;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String calculateRiskLevel(PortfolioAnalysisContext context) {
        if (context.getHoldingCount() <= 2 || context.getMaxHoldingWeight().compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "HIGH";
        }
        if (context.getHoldingCount() <= 4 || context.getMaxHoldingWeight().compareTo(BigDecimal.valueOf(40)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private List<String> buildRiskPoints(PortfolioAnalysisContext context) {
        List<String> points = new ArrayList<>();

        if (context.getHoldingCount() <= 2) {
            points.add("持仓股票数量较少，组合分散度偏低。");
        }
        if (context.getMaxHoldingWeight().compareTo(BigDecimal.valueOf(50)) >= 0) {
            points.add("单一持仓权重较高，组合集中度偏高。");
        }
        if (points.isEmpty()) {
            points.add("当前组合从权重分布来看相对均衡，但仍需持续关注市场波动和个股风险。");
        }

        return points;
    }

    private List<String> buildBasicSuggestions(PortfolioAnalysisContext context) {
        List<String> suggestions = new ArrayList<>();

        if (context.getHoldingCount() <= 2) {
            suggestions.add("可考虑增加不同标的，提升组合分散度。");
        }
        if (context.getMaxHoldingWeight().compareTo(BigDecimal.valueOf(50)) >= 0) {
            suggestions.add("可考虑适度降低单一重仓标的占比，优化仓位结构。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前组合权重结构相对均衡，可继续结合市场情况动态调整。");
        }

        return suggestions;
    }
}
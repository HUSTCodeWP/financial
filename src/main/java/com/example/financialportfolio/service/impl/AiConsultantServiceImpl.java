package com.example.financialportfolio.service.impl;

import com.example.financialportfolio.client.LlmClient;
import com.example.financialportfolio.dto.HoldingAnalysisItem;
import com.example.financialportfolio.dto.PortfolioAnalysisContext;
import com.example.financialportfolio.entity.Portfolio;
import com.example.financialportfolio.entity.PortfolioItem;
import com.example.financialportfolio.entity.Stock;
import com.example.financialportfolio.prompt.PromptBuilder;
import com.example.financialportfolio.repository.PortfolioItemRepository;
import com.example.financialportfolio.repository.PortfolioRepository;
import com.example.financialportfolio.repository.StockRepository;
import com.example.financialportfolio.service.AiConsultantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public SseEmitter generatePortfolioAdvice(Long portfolioId) {
        SseEmitter emitter = new SseEmitter(120000L);

        new Thread(() -> {
            try {
                PortfolioAnalysisContext context = buildPortfolioContext(portfolioId);
                String prompt = promptBuilder.buildPortfolioAdvicePrompt(context);

                emitter.send(SseEmitter.event().name("riskLevel").data(calculateRiskLevel(context)));

                llmClient.streamChat(prompt, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                emitter.send(SseEmitter.event().name("riskPoints")
                        .data(objectMapper.writeValueAsString(buildRiskPoints(context))));
                emitter.send(SseEmitter.event().name("suggestions")
                        .data(objectMapper.writeValueAsString(buildBasicSuggestions(context))));
                emitter.send(SseEmitter.event().name("disclaimer")
                        .data("For informational purposes only. This does not constitute investment advice."));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(
                            e.getMessage() == null ? "AI analysis failed." : e.getMessage()
                    ));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @Override
    public String answerPortfolioQuestion(Long portfolioId, String question, String riskPreference) {
        PortfolioAnalysisContext context = buildPortfolioContext(portfolioId);
        String prompt = promptBuilder.buildPortfolioQuestionPrompt(context, question, riskPreference);
        return llmClient.chat(prompt);
    }

    private PortfolioAnalysisContext buildPortfolioContext(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));

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
                if (stock.getEnglishName() != null && !stock.getEnglishName().isBlank()) {
                    stockName = stock.getEnglishName();
                } else {
                    stockName = stock.getChineseName();
                }
            }

            HoldingAnalysisItem analysisItem = new HoldingAnalysisItem();
            analysisItem.setStockCode(stockCode);
            analysisItem.setStockName(stockName);
            analysisItem.setWeight(weight);

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
            points.add("The number of holdings is relatively small, which suggests limited diversification.");
        }
        if (context.getMaxHoldingWeight().compareTo(BigDecimal.valueOf(50)) >= 0) {
            points.add("A single holding has a relatively high weight, indicating concentration risk.");
        }
        if (points.isEmpty()) {
            points.add("The portfolio structure appears relatively balanced, but market and single-stock risks still need monitoring.");
        }

        return points;
    }

    private List<String> buildBasicSuggestions(PortfolioAnalysisContext context) {
        List<String> suggestions = new ArrayList<>();

        if (context.getHoldingCount() <= 2) {
            suggestions.add("Consider adding more holdings to improve diversification.");
        }
        if (context.getMaxHoldingWeight().compareTo(BigDecimal.valueOf(50)) >= 0) {
            suggestions.add("Consider reducing the weight of the largest holding to optimize concentration risk.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("The portfolio structure is relatively balanced. Continue monitoring allocation changes and market conditions.");
        }

        return suggestions;
    }
}
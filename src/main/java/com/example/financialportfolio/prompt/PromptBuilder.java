package com.example.financialportfolio.prompt;

import com.example.financialportfolio.dto.HoldingAnalysisItem;
import com.example.financialportfolio.dto.PortfolioAnalysisContext;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Component
public class PromptBuilder {

    public String buildGeneralQuestionPrompt(String question) {
        return """
                请回答下面的投资相关问题：

                问题：
                %s

                要求：
                1. 用中文回答；
                2. 表达清晰、简洁、专业；
                3. 不能承诺收益；
                4. 不要给出绝对买卖结论；
                5. 回答尽量给出风险控制或组合优化思路；
                6. 最后附上一句风险提示。
                """.formatted(question);
    }

    public String buildPortfolioAdvicePrompt(PortfolioAnalysisContext context) {
        String holdingsText = buildHoldingsText(context);

        return """
                请根据以下投资组合结构数据，生成投资组合分析建议。

                【组合基础信息】
                组合ID：%s
                组合名称：%s
                持仓数量：%s
                最大单一持仓权重：%s%%

                【持仓明细】
                %s

                说明：
                1. 当前数据仅包含持仓股票及其权重；
                2. 不包含买入价格、买入数量、浮盈浮亏、收益率等信息；
                3. 请重点从组合集中度、分散化程度、结构均衡性、风险暴露角度分析。

                请严格按照以下格式输出：
                一、组合总体评价
                二、主要风险点
                三、优化建议
                四、风险提示

                要求：
                1. 用中文回答；
                2. 不承诺收益；
                3. 不给绝对买卖指令；
                4. 建议尽量具体、可操作；
                5. 适合普通投资者阅读。
                """.formatted(
                context.getPortfolioId(),
                context.getPortfolioName(),
                context.getHoldingCount(),
                context.getMaxHoldingWeight(),
                holdingsText
        );
    }

    public String buildPortfolioQuestionPrompt(PortfolioAnalysisContext context,
                                               String question,
                                               String riskPreference) {
        String holdingsText = buildHoldingsText(context);

        return """
                你现在要基于用户当前投资组合结构回答问题。

                【用户风险偏好】
                %s

                【组合基础信息】
                组合ID：%s
                组合名称：%s
                持仓数量：%s
                最大单一持仓权重：%s%%

                【持仓明细】
                %s

                【数据说明】
                当前数据仅包含股票代码、股票名称和仓位权重，
                不包含买入价、买入数量、浮盈浮亏和收益率等信息。

                【用户问题】
                %s

                请回答时遵守以下要求：
                1. 回答必须结合当前组合权重结构；
                2. 先直接回答问题，再解释原因；
                3. 可以给出分散化、仓位优化、风险控制方向建议；
                4. 不承诺收益，不提供绝对买卖指令；
                5. 如果问题涉及收益判断或具体买卖时机，要明确说明当前数据不足；
                6. 最后补一句风险提示。
                """.formatted(
                riskPreference == null ? "未提供" : riskPreference,
                context.getPortfolioId(),
                context.getPortfolioName(),
                context.getHoldingCount(),
                context.getMaxHoldingWeight(),
                holdingsText,
                question
        );
    }

    private String buildHoldingsText(PortfolioAnalysisContext context) {
        StringJoiner joiner = new StringJoiner("\n");
        for (HoldingAnalysisItem item : context.getHoldings()) {
            joiner.add(String.format(
                    "股票代码：%s，名称：%s，仓位权重：%s%%",
                    item.getStockCode(),
                    item.getStockName(),
                    item.getWeight()
            ));
        }
        return joiner.toString();
    }
}
package com.example.financialportfolio.prompt;

import com.example.financialportfolio.dto.HoldingAnalysisItem;
import com.example.financialportfolio.dto.PortfolioAnalysisContext;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Component
public class PromptBuilder {

    public String buildGeneralQuestionPrompt(String question) {
        return """
                 Please answer the following investment-related question in English.
                
                                              Question:
                                              %s
                
                                              Requirements:
                                              1. Respond in English only;
                                              2. Be clear, concise, and professional;
                                              3. Do not guarantee profits;
                                              4. Do not provide absolute buy/sell instructions;
                                              5. End with a brief risk reminder.
                """.formatted(question);
    }

    public String buildPortfolioAdvicePrompt(PortfolioAnalysisContext context) {
        String holdingsText = buildHoldingsText(context);

        return """
                Please analyze the following investment portfolio and provide advice in English.
                
                                [Portfolio Information]
                                Portfolio ID: %s
                                Portfolio Name: %s
                                Holding Count: %s
                                Maximum Single Holding Weight: %s%%
                
                                [Holdings]
                                %s
                
                                Notes:
                                1. Current available data mainly reflects portfolio structure and weight distribution;
                                2. Focus on concentration risk, diversification, and structural balance;
                                3. Respond entirely in English.
                
                                Please structure the answer with these sections:
                                1. Overall Assessment
                                2. Main Risk Points
                                3. Optimization Suggestions
                                4. Risk Reminder

                                Requirements:
                                1. Answer in English;
                                2. Do not promise returns;
                                3. Do not give absolute buy or sell instructions;
                                4. Suggestions should be as specific and actionable as possible;
                                5. Suitable for ordinary investors to read.
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
                You now need to answer questions based on the user's current portfolio structure.
                
                【User Risk Preference】
                %s
                
                【Portfolio Basic Information】
                Portfolio ID: %s
                Portfolio Name: %s
                Number of Holdings: %s
                Maximum Single Holding Weight: %s%%
                
                【Holding Details】
                %s
                
                【Data Description】
                The current data only includes stock codes, stock names, and position weights,
                without information on purchase price, purchase quantity, unrealized gains or losses, and returns.
                
                【User Question】
                %s
                
                Please follow the requirements when answering:
                1. Answers must be based on the current portfolio weight structure;
                2. Directly answer the question first, then explain the reason;
                3. You may provide suggestions on diversification, position optimization, and risk control;
                4. Do not promise returns or provide absolute buy/sell instructions;
                5. If the question involves return judgment or specific trading timing, clearly state that current data is insufficient;
                6. Finally, add a risk reminder.
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
                    "Stock Code: %s, Name: %s, Weight: %s%%",
                    item.getStockCode(),
                    item.getStockName(),
                    item.getWeight()
            ));
        }
        return joiner.toString();
    }
}
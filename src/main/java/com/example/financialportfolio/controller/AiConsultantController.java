package com.example.financialportfolio.controller;

import com.example.financialportfolio.common.result.ApiResponse;
import com.example.financialportfolio.dto.*;
import com.example.financialportfolio.service.AiConsultantService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5175")
@RestController
@RequestMapping("/api/ai")
public class AiConsultantController {

    private final AiConsultantService aiConsultantService;

    public AiConsultantController(AiConsultantService aiConsultantService) {
        this.aiConsultantService = aiConsultantService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        String answer = aiConsultantService.chat(request.getQuestion());
        return ApiResponse.success(new AiChatResponse(answer));
    }

    @PostMapping("/portfolio/{portfolioId}/advice")
    public ApiResponse<PortfolioAdviceResponse> generateAdvice(@PathVariable Long portfolioId) {
        PortfolioAdviceResponse response = aiConsultantService.generatePortfolioAdvice(portfolioId);
        return ApiResponse.success(response);
    }

    @PostMapping("/portfolio/{portfolioId}/question")
    public ApiResponse<PortfolioQuestionResponse> askPortfolioQuestion(@PathVariable Long portfolioId,
                                                                       @RequestBody PortfolioQuestionRequest request) {
        String answer = aiConsultantService.answerPortfolioQuestion(
                portfolioId,
                request.getQuestion(),
                request.getRiskPreference()
        );
        return ApiResponse.success(new PortfolioQuestionResponse(answer));
    }
}
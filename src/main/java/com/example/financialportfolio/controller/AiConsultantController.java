package com.example.financialportfolio.controller;

import com.example.financialportfolio.common.result.ApiResponse;
import com.example.financialportfolio.dto.*;
import com.example.financialportfolio.service.AiConsultantService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@CrossOrigin(origins = "*")
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

    @PostMapping(value = "/portfolio/{portfolioId}/advice", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateAdvice(@PathVariable Long portfolioId) {
        return aiConsultantService.generatePortfolioAdvice(portfolioId);
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
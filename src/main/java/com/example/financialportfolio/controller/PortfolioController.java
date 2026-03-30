package com.example.financialportfolio.controller;

import com.example.financialportfolio.common.result.ApiResponse;
import com.example.financialportfolio.dto.*;
import com.example.financialportfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ApiResponse<List<PortfolioListItemResponse>> getAllPortfolios() {
        return ApiResponse.success(portfolioService.getAllPortfolios(), "return successful");
    }

    @GetMapping("/{id}")
    public ApiResponse<PortfolioDetailResponse> getPortfolioById(@PathVariable Long id) {
        return ApiResponse.success(portfolioService.getPortfolioById(id), "portfolio fetched successfully");
    }

    // 新路径
    @PostMapping
    public ApiResponse<PortfolioOperationResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ApiResponse.success(
                portfolioService.createPortfolio(request),
                "portfolio created successfully"
        );
    }

    // 兼容旧路径
    @PostMapping("/createPortfolio")
    public ApiResponse<PortfolioOperationResponse> createPortfolioLegacy(
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ApiResponse.success(
                portfolioService.createPortfolio(request),
                "portfolio created successfully"
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<PortfolioOperationResponse> updatePortfolio(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        return ApiResponse.success(
                portfolioService.updatePortfolio(id, request),
                "portfolio updated successfully"
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ApiResponse.success(null, "portfolio deleted successfully");
    }
}
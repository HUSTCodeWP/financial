package com.example.financialportfolio.controller;

import com.example.financialportfolio.common.result.ApiResponse;
import com.example.financialportfolio.dto.CreatePortfolioRequest;
import com.example.financialportfolio.dto.PortfolioDetailResponse;
import com.example.financialportfolio.dto.PortfolioListItemResponse;
import com.example.financialportfolio.dto.PortfolioOperationResponse;
import com.example.financialportfolio.dto.UpdatePortfolioRequest;
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
        return ApiResponse.success(portfolioService.getAllPortfolios(), "查询成功");
    }

    @GetMapping("/{portfolioId}")
    public ApiResponse<PortfolioDetailResponse> getPortfolioById(@PathVariable Long portfolioId) {
        return ApiResponse.success(
                portfolioService.getPortfolioById(portfolioId),
                "查询组合详情成功"
        );
    }

    @PostMapping
    public ApiResponse<PortfolioOperationResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ApiResponse.success(
                portfolioService.createPortfolio(request),
                "创建组合成功"
        );
    }

    @PostMapping("/createPortfolio")
    public ApiResponse<PortfolioOperationResponse> createPortfolioLegacy(
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ApiResponse.success(
                portfolioService.createPortfolio(request),
                "创建组合成功"
        );
    }

    @PutMapping("/{portfolioId}")
    public ApiResponse<PortfolioOperationResponse> updatePortfolio(
            @PathVariable Long portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        return ApiResponse.success(
                portfolioService.updatePortfolio(portfolioId, request),
                "更新组合成功"
        );
    }

    @DeleteMapping("/{portfolioId}")
    public ApiResponse<Void> deletePortfolio(@PathVariable Long portfolioId) {
        portfolioService.deletePortfolio(portfolioId);
        return ApiResponse.success(null, "删除组合成功");
    }
}
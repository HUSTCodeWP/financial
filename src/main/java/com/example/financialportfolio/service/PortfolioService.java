package com.example.financialportfolio.service;

import com.example.financialportfolio.dto.*;

import java.util.List;

public interface PortfolioService {

    List<PortfolioListItemResponse> getAllPortfolios();

    PortfolioDetailResponse getPortfolioById(Long id);

    PortfolioOperationResponse createPortfolio(CreatePortfolioRequest request);

    PortfolioOperationResponse updatePortfolio(Long id, UpdatePortfolioRequest request);

    void deletePortfolio(Long id);
}
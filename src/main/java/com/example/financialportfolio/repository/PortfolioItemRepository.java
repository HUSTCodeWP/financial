package com.example.financialportfolio.repository;

import com.example.financialportfolio.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findByPortfolioId(Long portfolioId);
}
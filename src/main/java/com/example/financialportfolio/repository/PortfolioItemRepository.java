package com.example.financialportfolio.repository;

import com.example.financialportfolio.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
}
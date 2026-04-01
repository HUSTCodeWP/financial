package com.example.financialportfolio.repository;

import com.example.financialportfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @Query("select p from Portfolio p left join fetch p.items where p.id = :id")
    Optional<Portfolio> findByIdWithItems(Long id);
}
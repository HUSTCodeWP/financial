package com.example.financialportfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePortfolioRequest {

    @NotBlank(message = "Portfolio name must not be blank")
    private String name;

    @Valid
    @NotEmpty(message = "Portfolio details must not be empty")
    private List<PortfolioDetailItemDto> details;
}
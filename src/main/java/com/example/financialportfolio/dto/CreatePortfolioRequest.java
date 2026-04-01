package com.example.financialportfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreatePortfolioRequest {

    @NotBlank(message = "Portfolio name must not be blank")
    private String name;

    // 可选：如果前端不传，则后端使用当前时间
    private LocalDateTime timestamp;

    @Valid
    @NotEmpty(message = "Portfolio details must not be empty")
    private List<PortfolioDetailItemDto> details;
}
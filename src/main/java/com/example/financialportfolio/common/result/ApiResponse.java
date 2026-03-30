package com.example.financialportfolio.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T result;
    private String message;

    public static <T> ApiResponse<T> success(T result, String message) {
        return new ApiResponse<>(true, result, message);
    }

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(true, result, "success");
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
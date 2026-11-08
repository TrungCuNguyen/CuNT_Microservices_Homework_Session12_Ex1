package com.example.productservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;
    private Long responseTimeMs;
    private String dataSource; // "DATABASE" hoặc "REDIS_CACHE" (hoặc từ cache header)
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message, Long responseTimeMs, String dataSource) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .responseTimeMs(responseTimeMs)
                .dataSource(dataSource)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .status(201)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}


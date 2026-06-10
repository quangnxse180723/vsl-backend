package com.vslbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Bao boc chuan cho moi response THANH CONG.
 * <pre>
 * {
 *   "success": true,
 *   "code": "SUCCESS",
 *   "message": "Dang nhap thanh cong",
 *   "data": { ... },
 *   "timestamp": "2026-06-10T08:00:00Z"
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private final boolean success = true;

    @Builder.Default
    private final String code = "SUCCESS";

    private final String message;
    private final T data;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> of(String message, T data) {
        return ApiResponse.<T>builder().message(message).data(data).build();
    }

    public static <T> ApiResponse<T> of(String message) {
        return ApiResponse.<T>builder().message(message).build();
    }
}

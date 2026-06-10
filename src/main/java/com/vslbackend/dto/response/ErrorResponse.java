package com.vslbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Cau truc JSON chuan cho moi loi tra ve client.
 * <pre>
 * {
 *   "timestamp": "2026-06-10T08:00:00Z",
 *   "status": 409,
 *   "error": "EMAIL_ALREADY_EXISTS",
 *   "code": "AUTH_1001",
 *   "message": "Email da duoc su dung",
 *   "path": "/api/auth/register",
 *   "errors": [ { "field": "email", "message": "..." } ]
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    /** Loai loi (ten ErrorCode), VD: EMAIL_ALREADY_EXISTS. */
    private final String error;
    /** Ma loi nghiep vu, VD: AUTH_1001. */
    private final String code;
    private final String message;
    private final String path;
    /** Chi co mat khi loi validation tung truong. */
    private final List<FieldError> errors;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}

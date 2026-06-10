package com.vslbackend.exception;

import com.vslbackend.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Diem bat loi tap trung cho toan he thong - moi loi deu tra ve {@link ErrorResponse} chuan JSON.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Loi nghiep vu da biet truoc. */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode ec = ex.getErrorCode();
        return build(ec.getStatus(), ec.name(), ec.getCode(), ex.getMessage(), request, null);
    }

    /** Loi validation @Valid tren request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        ErrorCode ec = ErrorCode.VALIDATION_ERROR;
        return build(ec.getStatus(), ec.name(), ec.getCode(), ec.getMessage(), request, fieldErrors);
    }

    /** Sai email/mat khau khi dang nhap (nem tu AuthenticationManager). */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(AuthenticationException ex,
                                                             HttpServletRequest request) {
        ErrorCode ec = ErrorCode.INVALID_CREDENTIALS;
        return build(ec.getStatus(), ec.name(), ec.getCode(), ec.getMessage(), request, null);
    }

    /** Khong du quyen (vi du USER goi endpoint ADMIN). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        ErrorCode ec = ErrorCode.ACCESS_DENIED;
        return build(ec.getStatus(), ec.name(), ec.getCode(), ec.getMessage(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        ErrorCode ec = ErrorCode.METHOD_NOT_ALLOWED;
        return build(ec.getStatus(), ec.name(), ec.getCode(), ec.getMessage(), request, null);
    }

    /** Luoi an toan cuoi cung - khong lo stack trace ra ngoai. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Loi khong xac dinh tai {}: ", request.getRequestURI(), ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return build(ec.getStatus(), ec.name(), ec.getCode(), ec.getMessage(), request, null);
    }

    // ------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String error,
                                                String code,
                                                String message,
                                                HttpServletRequest request,
                                                List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

package com.loyalty.mediation.exception;

import com.loyalty.mediation.logging.SplunkLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SplunkLogger splunkLogger;

    /**
     * Bean validation failures (e.g. @NotBlank, @Past).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + details);
    }

    /**
     * Vendor API returned 4xx or 5xx.
     */
    @ExceptionHandler(VendorClientException.class)
    public ResponseEntity<Map<String, Object>> handleVendorError(VendorClientException ex) {
        int vendorStatus = ex.getVendorStatusCode();

        splunkLogger.error(
                "VENDOR_ERROR",
                "n/a",
                ex.getMessage(),
                Map.of("vendorHttpStatus", vendorStatus)
        );

        HttpStatus responseStatus = switch (vendorStatus) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 422 -> HttpStatus.UNPROCESSABLE_ENTITY;
            default  -> HttpStatus.BAD_GATEWAY;
        };

        return buildErrorResponse(responseStatus, ex.getMessage());
    }

    /**
     * Catch-all for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        splunkLogger.error("UNEXPECTED_ERROR", "n/a", ex.getMessage(), Map.of());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

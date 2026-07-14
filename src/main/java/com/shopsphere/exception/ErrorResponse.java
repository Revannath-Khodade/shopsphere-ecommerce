package com.shopsphere.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard JSON shape returned for every error response across the API:
 * {
 *   "timestamp": "2026-07-10T10:15:30",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Product not found with id: '42'",
 *   "path": "/api/products/42",
 *   "validationErrors": { "email": "must be a well-formed email address" }
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** Populated only for validation failures: field name -> error message. */
    private Map<String, String> validationErrors;
}

package com.shopsphere.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Invoked by Spring Security whenever an unauthenticated request hits a
 * protected endpoint (missing token, invalid token, expired token). Produces
 * the same {@link ErrorResponse} JSON shape used everywhere else in the API,
 * so clients don't need a special case just for auth failures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

        log.warn("Unauthorized request to [{}]: {}", request.getRequestURI(), authException.getMessage());

        // The specific reason (missing/expired/invalid token) is set as a
        // request attribute by JwtAuthenticationFilter before it lets the
        // request continue to this entry point - fall back to a generic
        // message if the filter never ran (e.g. malformed request path).
        Object detailedReason = request.getAttribute("jwt_exception_message");
        String message = detailedReason != null
                ? detailedReason.toString()
                : "Authentication is required to access this resource.";

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

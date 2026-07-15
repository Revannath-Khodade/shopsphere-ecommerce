package com.shopsphere.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request, ahead of Spring Security's standard username/password
 * filter. Extracts a bearer JWT (if present), validates it, loads the
 * corresponding {@link UserDetails}, and - if everything checks out -
 * populates the {@link SecurityContextHolder} so downstream authorization
 * (@PreAuthorize, requestMatchers, etc.) sees an authenticated principal.
 * <p>
 * On any JWT problem (missing, malformed, expired, wrong type) the filter
 * does NOT throw - it simply leaves the SecurityContext empty and lets the
 * request continue. Spring Security's access-control rules (and ultimately
 * {@link JwtAuthenticationEntryPoint}) are what turn "no authentication" into
 * a 401, keeping this filter focused solely on authentication, not authorization.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JWT_EXCEPTION_ATTRIBUTE = "jwt_exception_message";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateWithToken(token, request);
        } catch (ExpiredJwtException e) {
            log.debug("Rejected expired JWT for request [{}]", request.getRequestURI());
            request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, "Your session has expired. Please log in again.");
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected invalid JWT for request [{}]: {}", request.getRequestURI(), e.getMessage());
            request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, "Invalid authentication token.");
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateWithToken(String token, HttpServletRequest request) {
        String username = jwtService.extractUsername(token);

        boolean alreadyAuthenticated = SecurityContextHolder.getContext().getAuthentication() != null;
        if (username == null || alreadyAuthenticated) {
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isAccessTokenValid(token, userDetails)) {
            request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, "Invalid or expired authentication token.");
            return;
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        log.debug("Authenticated request [{}] as user='{}'", request.getRequestURI(), username);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

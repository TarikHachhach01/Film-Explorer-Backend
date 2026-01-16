package com.isfa.dsi.filmexplorer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String requestPath = request.getRequestURI();
        final String requestMethod = request.getMethod();

        log.debug("🔍 Processing request: {} {}", requestMethod, requestPath);

        try {

            String jwt = getTokenFromCookie(request);


            if (jwt == null) {
                jwt = getTokenFromAuthHeader(request);
            }


            if (jwt == null) {
                log.debug("Skipping JWT authentication - no token found in cookie or header");
                filterChain.doFilter(request, response);
                return;
            }


            if (!isValidTokenFormat(jwt)) {
                log.error(" Invalid JWT format!");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract the username (email) from the token
            final String userEmail = jwtService.extractUserName(jwt);
            log.debug("Extracted email from token: {}", userEmail);

            // If we have an email and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("Loading user details for: {}", userEmail);

                // Load user details from database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.debug("User details loaded: {}", userDetails.getUsername());

                // Validate the token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    log.info(" Token valid for user: {}", userEmail);

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Add request details
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("User {} authenticated successfully for: {} {}",
                            userEmail, requestMethod, requestPath);
                } else {
                    log.warn(" Token validation failed for user: {}", userEmail);
                }
            } else if (userEmail == null) {
                log.warn(" Could not extract email from token");
            } else {
                log.debug("User already authenticated in security context");
            }
        } catch (Exception e) {
            log.error(" Cannot set user authentication: {}", e.getMessage());
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Request: {} {}", requestMethod, requestPath);

            // Log stack trace for debugging
            if (log.isDebugEnabled()) {
                log.debug("Full stack trace:", e);
            }
        }

        filterChain.doFilter(request, response);
    }


    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            log.debug("No cookies found in request");
            return null;
        }

        // Chercher le cookie 'accessToken'
        for (Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName())) {
                log.debug(" Token found in HttpOnly Cookie");
                return cookie.getValue();
            }
        }

        log.debug(" No accessToken cookie found");
        return null;
    }


    private String getTokenFromAuthHeader(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        // Debug logging
        if (authHeader == null) {
            log.debug(" No Authorization header found");
            return null;
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.debug(" Authorization header does not start with 'Bearer '");
            return null;
        }

        log.debug(" Authorization header found");

        return authHeader.substring(7);
    }


    private boolean isValidTokenFormat(String jwt) {
        // Check token format (should have 2 dots for 3 parts: header.payload.signature)
        int dotCount = jwt.length() - jwt.replace(".", "").length();

        log.debug("Token length: {}", jwt.length());
        log.debug("Token preview: {}...", jwt.substring(0, Math.min(jwt.length(), 30)));
        log.debug("Token dot count: {} (should be 2)", dotCount);

        if (dotCount != 2) {
            log.error(" Invalid JWT format! Expected 2 dots, found: {}", dotCount);
            log.error("Token: {}", jwt);
            return false;
        }

        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Don't filter public endpoints
        boolean shouldSkip = path.startsWith("/api/auth/") ||
                (path.startsWith("/api/movies/") && request.getMethod().equals("GET")) ||
                (path.equals("/api/movies/search") && request.getMethod().equals("POST"));

        if (shouldSkip) {
            log.debug(" Skipping JWT filter for public endpoint: {} {}", request.getMethod(), path);
        }

        return shouldSkip;
    }
}
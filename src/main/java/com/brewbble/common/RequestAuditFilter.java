package com.brewbble.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestAuditFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri    = request.getRequestURI();

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int  status  = response.getStatus();

            if (status >= 500) {
                log.error("API {} {} → {} ({}ms)", method, uri, status, elapsed);
            } else if (status >= 400) {
                log.warn("API  {} {} → {} ({}ms)", method, uri, status, elapsed);
            } else {
                log.info("API  {} {} → {} ({}ms)", method, uri, status, elapsed);
            }
        }
    }

    /** Skip actuator/health endpoints to keep logs clean. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.equals("/api/v1/health");
    }
}

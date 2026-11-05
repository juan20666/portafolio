package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Lets the portfolio (served from GitHub Pages) call this API from the browser.
 *
 * <p>It is a filter, not a Spring MVC configuration, so the CORS headers are also present
 * on the answers produced by other filters, such as a 429 from the rate limiter.
 */
public class CorsPolicyFilter extends OncePerRequestFilter implements Ordered {

    private static final String API_PREFIX = "/api/";

    private final Set<String> allowedOrigins;

    /** @param allowedOrigins exact origins (scheme + host [+ port]) that may call the API */
    public CorsPolicyFilter(Collection<String> allowedOrigins) {
        this.allowedOrigins = Set.copyOf(allowedOrigins);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        response.addHeader("Vary", "Origin");

        boolean preflight = "OPTIONS".equals(request.getMethod())
                && request.getHeader("Access-Control-Request-Method") != null;

        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            if (preflight) {
                response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
                response.setHeader("Access-Control-Max-Age", "3600");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
        } else if (preflight) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}

package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Protects the public contact endpoint from floods and oversized bodies. */
public class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    static final String PROTECTED_PATH = "/api/v1/contact-messages";
    static final long MAX_BODY_BYTES = 16 * 1024;

    private static final String EVERYBODY = "*";

    private final FixedWindowRateLimiter perClient;
    private final FixedWindowRateLimiter global;
    private final int trustedProxyHops;

    /**
     * @param maxRequestsPerClient requests allowed per client address in each window
     * @param maxRequestsGlobal    requests allowed from everybody together in each window
     * @param trustedProxyHops     see {@link ClientAddress#resolve}
     */
    public RateLimitFilter(int maxRequestsPerClient, int maxRequestsGlobal, Duration window, int trustedProxyHops) {
        this.perClient = new FixedWindowRateLimiter(maxRequestsPerClient, window);
        this.global = new FixedWindowRateLimiter(maxRequestsGlobal, window);
        this.trustedProxyHops = trustedProxyHops;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && PROTECTED_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            reject(response, 413, "Mensaje demasiado grande", "El cuerpo de la solicitud supera el límite permitido.");
            return;
        }

        String client = ClientAddress.resolve(
                request.getHeader("X-Forwarded-For"), request.getRemoteAddr(), trustedProxyHops);
        long now = System.currentTimeMillis();
        FixedWindowRateLimiter.Decision decision = perClient.tryAcquire(client, now);
        if (decision.allowed()) {
            decision = global.tryAcquire(EVERYBODY, now);
        }
        if (!decision.allowed()) {
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            reject(response, 429, "Demasiadas solicitudes", "Espera un momento antes de volver a intentarlo.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Fixed texts without quotes or backslashes: safe to write without escaping.
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title
                + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\",\"errors\":[]}");
    }
}

package dev.juanpablocorrea.portfolio.contact.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Limits for the public contact endpoint.
 *
 * @param maxRequests       requests allowed per client in each window
 * @param globalMaxRequests requests allowed from everybody together in each window (a safety net
 *                          in case the client address cannot be told apart)
 * @param window            length of the window
 * @param trustedProxyHops  how many entries, counted from the right of {@code X-Forwarded-For},
 *                          were added by infrastructure you trust
 */
@ConfigurationProperties(prefix = "portfolio.rate-limit")
public record RateLimitProperties(int maxRequests, int globalMaxRequests, Duration window, int trustedProxyHops) {

    public RateLimitProperties {
        maxRequests = maxRequests > 0 ? maxRequests : 5;
        globalMaxRequests = globalMaxRequests > 0 ? globalMaxRequests : 30;
        window = window == null || window.isZero() || window.isNegative() ? Duration.ofMinutes(1) : window;
        trustedProxyHops = Math.max(trustedProxyHops, 0);
    }
}

package dev.juanpablocorrea.portfolio.contact.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origins (scheme + host, no path) allowed to call the API from a browser,
 * for example {@code https://juanpablo.github.io}.
 */
@ConfigurationProperties(prefix = "portfolio.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::strip)
                        .map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
                        .filter(origin -> !origin.isEmpty())
                        .toList();
    }
}

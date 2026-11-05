package dev.juanpablocorrea.portfolio.contact.infrastructure.config;

import dev.juanpablocorrea.portfolio.contact.application.port.in.NotifyOwnerUseCase;
import dev.juanpablocorrea.portfolio.contact.application.port.in.ReceiveContactMessageUseCase;
import dev.juanpablocorrea.portfolio.contact.application.port.out.DomainEventPublisher;
import dev.juanpablocorrea.portfolio.contact.application.port.out.OwnerNotifier;
import dev.juanpablocorrea.portfolio.contact.application.service.NotifyOwnerService;
import dev.juanpablocorrea.portfolio.contact.application.service.ReceiveContactMessageService;
import dev.juanpablocorrea.portfolio.contact.domain.repository.ContactMessageRepository;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter.CorsPolicyFilter;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter.RateLimitFilter;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter.SecurityHeadersFilter;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.notification.LogOnlyOwnerNotifier;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.notification.ResendOwnerNotifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * Composition root: the only place that knows which adapter plugs into which port.
 * The domain and application layers stay free of Spring and are wired here by hand.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties({CorsProperties.class, RateLimitProperties.class, NotificationProperties.class})
public class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ReceiveContactMessageUseCase receiveContactMessageUseCase(
            ContactMessageRepository repository, DomainEventPublisher publisher, Clock clock) {
        return new ReceiveContactMessageService(repository, publisher, clock);
    }

    @Bean
    NotifyOwnerUseCase notifyOwnerUseCase(OwnerNotifier notifier) {
        return new NotifyOwnerService(notifier);
    }

    @Bean
    OwnerNotifier ownerNotifier(NotificationProperties properties) {
        if (!properties.emailConfigured()) {
            return new LogOnlyOwnerNotifier();
        }
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        return new ResendOwnerNotifier(
                http,
                URI.create(properties.apiUrl()),
                properties.apiKey(),
                properties.from(),
                properties.to(),
                1500);
    }

    @Bean
    SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    CorsPolicyFilter corsPolicyFilter(CorsProperties properties) {
        return new CorsPolicyFilter(properties.allowedOrigins());
    }

    @Bean
    RateLimitFilter rateLimitFilter(RateLimitProperties properties) {
        return new RateLimitFilter(
                properties.maxRequests(),
                properties.globalMaxRequests(),
                properties.window(),
                properties.trustedProxyHops());
    }
}

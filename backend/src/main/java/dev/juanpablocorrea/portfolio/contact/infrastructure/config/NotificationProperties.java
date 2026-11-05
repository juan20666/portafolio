package dev.juanpablocorrea.portfolio.contact.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How the owner is told about a new message. Without {@code apiKey} and {@code to}
 * the app only writes a line in the log, which is enough for local development.
 *
 * @param apiKey Resend API key
 * @param from   sender, for example {@code Portafolio <onboarding@resend.dev>}
 * @param to     the owner's address (with onboarding@resend.dev it must be the Resend account's email)
 * @param apiUrl endpoint of the Resend "send email" API
 */
@ConfigurationProperties(prefix = "portfolio.notification")
public record NotificationProperties(String apiKey, String from, String to, String apiUrl) {

    public NotificationProperties {
        apiKey = apiKey == null ? "" : apiKey.strip();
        from = from == null || from.isBlank() ? "Portafolio <onboarding@resend.dev>" : from.strip();
        to = to == null ? "" : to.strip();
        apiUrl = apiUrl == null || apiUrl.isBlank() ? "https://api.resend.com/emails" : apiUrl.strip();
    }

    public boolean emailConfigured() {
        return !apiKey.isEmpty() && !to.isEmpty();
    }
}

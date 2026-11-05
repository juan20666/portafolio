package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.notification;

import dev.juanpablocorrea.portfolio.contact.application.port.out.OwnerNotifier;
import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Driven adapter: emails the owner through Resend's HTTPS API.
 *
 * <p>HTTPS is used on purpose: free hosting plans such as Render's block SMTP ports.
 * The visitor's address goes in {@code reply_to}, so answering the email answers the visitor.
 * It retries a few times on network errors, 429 and 5xx, and the idempotency key
 * prevents Resend from sending the same notification twice.
 */
public class ResendOwnerNotifier implements OwnerNotifier {

    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient http;
    private final URI endpoint;
    private final String apiKey;
    private final String from;
    private final String to;
    private final long retryDelayMillis;

    public ResendOwnerNotifier(
            HttpClient http, URI endpoint, String apiKey, String from, String to, long retryDelayMillis) {
        this.http = http;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.from = from;
        this.to = to;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public void notifyNewMessage(ContactMessageReceived event) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "contact-message-" + event.messageId().value())
                .POST(HttpRequest.BodyPublishers.ofString(buildJson(event)))
                .build();

        NotificationFailedException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                if (status >= 200 && status < 300) {
                    return;
                }
                lastFailure = new NotificationFailedException("Resend answered HTTP " + status);
                if (status != 429 && status < 500) {
                    throw lastFailure; // a client error will not get better by retrying
                }
            } catch (IOException networkFailure) {
                lastFailure = new NotificationFailedException(
                        "Resend could not be reached: " + networkFailure.getClass().getSimpleName(), networkFailure);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new NotificationFailedException("Interrupted while notifying the owner", interrupted);
            }
            pause(attempt);
        }
        throw lastFailure;
    }

    private void pause(int attempt) {
        if (attempt >= MAX_ATTEMPTS || retryDelayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(retryDelayMillis * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new NotificationFailedException("Interrupted while notifying the owner", interrupted);
        }
    }

    String buildJson(ContactMessageReceived event) {
        String subject = "Portafolio · " + event.reason().label() + " · " + event.senderName().value();
        String text = "Nuevo mensaje desde tu portafolio\n\n"
                + "De: " + event.senderName().value() + " <" + event.senderEmail().value() + ">\n"
                + "Motivo: " + event.reason().label() + "\n"
                + "Recibido: " + event.occurredAt() + "\n\n"
                + event.body().value() + "\n";
        return "{\"from\":" + JsonStrings.quote(from)
                + ",\"to\":[" + JsonStrings.quote(to) + "]"
                + ",\"reply_to\":" + JsonStrings.quote(event.senderEmail().value())
                + ",\"subject\":" + JsonStrings.quote(subject)
                + ",\"text\":" + JsonStrings.quote(text) + "}";
    }
}

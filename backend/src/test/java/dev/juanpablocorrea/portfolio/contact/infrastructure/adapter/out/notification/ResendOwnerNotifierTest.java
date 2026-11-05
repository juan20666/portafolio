package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.notification;

import com.sun.net.httpserver.HttpServer;
import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.support.ContactFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Talks to a real HTTP server started by the JDK, so the request on the wire is what gets checked. */
class ResendOwnerNotifierTest {

    private HttpServer server;
    private final List<String> authorizations = Collections.synchronizedList(new ArrayList<>());
    private final List<String> idempotencyKeys = Collections.synchronizedList(new ArrayList<>());
    private final List<String> bodies = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int[] statuses = {200};

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", exchange -> {
            int call = calls.getAndIncrement();
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            idempotencyKeys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int status = statuses[Math.min(call, statuses.length - 1)];
            byte[] answer = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private ResendOwnerNotifier notifier() {
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/emails");
        return new ResendOwnerNotifier(
                HttpClient.newHttpClient(), endpoint, "re_test_key",
                "Portafolio <onboarding@resend.dev>", "owner@example.com", 0);
    }

    private static ContactMessageReceived event() {
        ContactMessage message = ContactMessage.receive(
                ContactFixtures.ID, "Ana \"La Jefa\" Pérez", "ana@example.com", "oferta",
                "Línea uno\nLínea dos con \"comillas\".", true, ContactFixtures.NOW);
        return (ContactMessageReceived) message.pullEvents().get(0);
    }

    @Test
    void sendsTheEmailWithTheRightHeadersAndBody() {
        notifier().notifyNewMessage(event());

        assertEquals(1, calls.get());
        assertEquals("Bearer re_test_key", authorizations.get(0));
        assertEquals("contact-message-" + ContactFixtures.ID, idempotencyKeys.get(0));
        String body = bodies.get(0);
        assertTrue(body.contains("\"to\":[\"owner@example.com\"]"));
        assertTrue(body.contains("\"reply_to\":\"ana@example.com\""));
        assertTrue(body.contains("Ana \\\"La Jefa\\\" Pérez"));
        assertTrue(body.contains("Línea uno\\nLínea dos con \\\"comillas\\\"."));
    }

    @Test
    void retriesWhenTheProviderFailsTemporarily() {
        statuses = new int[] {503, 429, 200};

        notifier().notifyNewMessage(event());

        assertEquals(3, calls.get());
    }

    @Test
    void doesNotRetryAfterAClientError() {
        statuses = new int[] {401};

        assertThrows(NotificationFailedException.class, () -> notifier().notifyNewMessage(event()));
        assertEquals(1, calls.get());
    }

    @Test
    void givesUpAfterThreeAttempts() {
        statuses = new int[] {500};

        assertThrows(NotificationFailedException.class, () -> notifier().notifyNewMessage(event()));
        assertEquals(3, calls.get());
    }
}

package dev.juanpablocorrea.portfolio.contact.domain;

import dev.juanpablocorrea.portfolio.contact.domain.error.ValidationFailedException;
import dev.juanpablocorrea.portfolio.contact.domain.error.Violation;
import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;
import dev.juanpablocorrea.portfolio.contact.domain.event.DomainEvent;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactReason;
import dev.juanpablocorrea.portfolio.contact.domain.model.EmailAddress;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageBody;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;
import dev.juanpablocorrea.portfolio.contact.domain.model.SenderName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactMessageTest {

    private static final String ID = "3f2b8c1e-5d4a-4e7b-9a10-2c6d8e0f1a23";
    private static final Instant NOW = Instant.parse("2026-11-03T11:00:00Z");
    private static final String BODY = "Hola, me gustaría conversar sobre una oportunidad.";

    private static ContactMessage valid() {
        return ContactMessage.receive(ID, "Ana Pérez", "ana@example.com", "oferta", BODY, true, NOW);
    }

    private static List<String> fieldsRejectedBy(Runnable action) {
        ValidationFailedException failure = assertThrows(ValidationFailedException.class, action::run);
        return failure.violations().stream().map(Violation::field).toList();
    }

    @Test
    void acceptsAValidMessage() {
        ContactMessage message = valid();

        assertEquals(MessageId.parse(ID), message.id());
        assertEquals("Ana Pérez", message.senderName().value());
        assertEquals(ContactReason.JOB_OFFER, message.reason());
        assertEquals(NOW, message.receivedAt());
    }

    @Test
    void raisesExactlyOneReceivedEventAndForgetsItAfterPullingIt() {
        ContactMessage message = valid();

        List<DomainEvent> first = message.pullEvents();
        List<DomainEvent> second = message.pullEvents();

        assertEquals(1, first.size());
        ContactMessageReceived event = assertInstanceOf(ContactMessageReceived.class, first.get(0));
        assertEquals(message.id(), event.messageId());
        assertEquals(NOW, event.occurredAt());
        assertTrue(second.isEmpty());
    }

    @Test
    void reportsEveryBrokenRuleAtOnce() {
        List<String> fields = fieldsRejectedBy(() ->
                ContactMessage.receive("nope", "A", "not-an-email", "???", "short", false, NOW));

        assertEquals(List.of("submissionId", "name", "email", "reason", "message", "consent"), fields);
    }

    @Test
    void refusesAMessageWithoutTheVisitorsAuthorisation() {
        List<String> fields = fieldsRejectedBy(() ->
                ContactMessage.receive(ID, "Ana Pérez", "ana@example.com", "oferta", BODY, false, NOW));

        assertEquals(List.of("consent"), fields);
    }

    @Test
    void aRestoredMessageRaisesNoEvents() {
        ContactMessage restored = ContactMessage.restore(
                MessageId.parse(ID), SenderName.of("Ana Pérez"), EmailAddress.of("ana@example.com"),
                ContactReason.OTHER, MessageBody.of(BODY), NOW);

        assertTrue(restored.pullEvents().isEmpty());
    }

    @Test
    void twoMessagesWithTheSameIdAreTheSameMessage() {
        ContactMessage first = valid();
        ContactMessage second = ContactMessage.receive(
                ID.toUpperCase(), "Otra Persona", "otra@example.com", "otro", "Otro mensaje distinto.", true, NOW);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}

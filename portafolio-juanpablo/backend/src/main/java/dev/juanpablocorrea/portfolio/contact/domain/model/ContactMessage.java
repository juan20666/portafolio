package dev.juanpablocorrea.portfolio.contact.domain.model;

import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;
import dev.juanpablocorrea.portfolio.contact.domain.event.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root: a message that a visitor leaves through the portfolio.
 *
 * <p>The only way to create a new one is {@link #receive}, so an invalid message
 * (or one without the visitor's authorisation) cannot exist.
 */
public final class ContactMessage {

    private final MessageId id;
    private final SenderName senderName;
    private final EmailAddress senderEmail;
    private final ContactReason reason;
    private final MessageBody body;
    private final Instant receivedAt;
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    private ContactMessage(
            MessageId id,
            SenderName senderName,
            EmailAddress senderEmail,
            ContactReason reason,
            MessageBody body,
            Instant receivedAt) {
        this.id = Objects.requireNonNull(id);
        this.senderName = Objects.requireNonNull(senderName);
        this.senderEmail = Objects.requireNonNull(senderEmail);
        this.reason = Objects.requireNonNull(reason);
        this.body = Objects.requireNonNull(body);
        this.receivedAt = Objects.requireNonNull(receivedAt);
    }

    // snippet:start
    /** Accepts a visitor's message, or reports every rule it breaks. */
    public static ContactMessage receive(
            String rawId,
            String rawName,
            String rawEmail,
            String rawReason,
            String rawBody,
            boolean consentGiven,
            Instant now) {

        Violations violations = new Violations();
        MessageId id = violations.collect("submissionId", () -> MessageId.parse(rawId));
        SenderName name = violations.collect("name", () -> SenderName.of(rawName));
        EmailAddress email = violations.collect("email", () -> EmailAddress.of(rawEmail));
        ContactReason reason = violations.collect("reason", () -> ContactReason.fromCode(rawReason));
        MessageBody body = violations.collect("message", () -> MessageBody.of(rawBody));
        violations.require(consentGiven, "consent",
                "Debes autorizar el tratamiento de tus datos para poder responderte.");
        violations.throwIfAny();

        ContactMessage message = new ContactMessage(id, name, email, reason, body, now);
        message.pendingEvents.add(new ContactMessageReceived(id, name, email, reason, body, now));
        return message;
    }
    // snippet:end

    /** Rebuilds a message that was stored earlier. It raises no events. */
    public static ContactMessage restore(
            MessageId id,
            SenderName senderName,
            EmailAddress senderEmail,
            ContactReason reason,
            MessageBody body,
            Instant receivedAt) {
        return new ContactMessage(id, senderName, senderEmail, reason, body, receivedAt);
    }

    /** Hands over the events raised since the last call and forgets them. */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    public MessageId id() {
        return id;
    }

    public SenderName senderName() {
        return senderName;
    }

    public EmailAddress senderEmail() {
        return senderEmail;
    }

    public ContactReason reason() {
        return reason;
    }

    public MessageBody body() {
        return body;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ContactMessage that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

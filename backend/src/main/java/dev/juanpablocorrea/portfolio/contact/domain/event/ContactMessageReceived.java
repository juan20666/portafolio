package dev.juanpablocorrea.portfolio.contact.domain.event;

import dev.juanpablocorrea.portfolio.contact.domain.model.ContactReason;
import dev.juanpablocorrea.portfolio.contact.domain.model.EmailAddress;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageBody;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;
import dev.juanpablocorrea.portfolio.contact.domain.model.SenderName;

import java.time.Instant;

/** A visitor's message was accepted and stored. */
public record ContactMessageReceived(
        MessageId messageId,
        SenderName senderName,
        EmailAddress senderEmail,
        ContactReason reason,
        MessageBody body,
        Instant occurredAt) implements DomainEvent {
}

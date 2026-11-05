package dev.juanpablocorrea.portfolio.contact.application.command;

import java.time.Instant;

/** Proof that the message was accepted. */
public record Receipt(String messageId, Instant receivedAt) {
}

package dev.juanpablocorrea.portfolio.contact.domain.event;

import java.time.Instant;

/** Something that happened in the domain and that other parts of the system may care about. */
public interface DomainEvent {

    Instant occurredAt();
}

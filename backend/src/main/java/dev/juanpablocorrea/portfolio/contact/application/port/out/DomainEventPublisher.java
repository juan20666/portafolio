package dev.juanpablocorrea.portfolio.contact.application.port.out;

import dev.juanpablocorrea.portfolio.contact.domain.event.DomainEvent;

import java.util.List;

/** Driven port: tells the rest of the system what happened. */
public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}

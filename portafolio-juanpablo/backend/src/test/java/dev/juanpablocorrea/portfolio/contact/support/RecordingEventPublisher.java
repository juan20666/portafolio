package dev.juanpablocorrea.portfolio.contact.support;

import dev.juanpablocorrea.portfolio.contact.application.port.out.DomainEventPublisher;
import dev.juanpablocorrea.portfolio.contact.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/** Fake publisher that remembers what it was asked to publish. */
public final class RecordingEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(List<DomainEvent> events) {
        published.addAll(events);
    }

    public List<DomainEvent> published() {
        return List.copyOf(published);
    }
}

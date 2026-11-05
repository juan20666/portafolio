package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.events;

import dev.juanpablocorrea.portfolio.contact.application.port.out.DomainEventPublisher;
import dev.juanpablocorrea.portfolio.contact.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/** Driven adapter: hands domain events to Spring's in-process event bus. */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher bus;

    public SpringDomainEventPublisher(ApplicationEventPublisher bus) {
        this.bus = bus;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(bus::publishEvent);
    }
}

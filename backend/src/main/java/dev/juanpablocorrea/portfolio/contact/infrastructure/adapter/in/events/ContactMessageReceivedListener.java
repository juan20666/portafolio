package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.events;

import dev.juanpablocorrea.portfolio.contact.application.port.in.NotifyOwnerUseCase;
import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Driving adapter fed by events instead of HTTP. It runs in the background, so a slow or
 * failing notification can never make the visitor wait or lose the message that was already saved.
 */
@Component
public class ContactMessageReceivedListener {

    private static final Logger LOG = LoggerFactory.getLogger(ContactMessageReceivedListener.class);

    private final NotifyOwnerUseCase notifyOwner;

    public ContactMessageReceivedListener(NotifyOwnerUseCase notifyOwner) {
        this.notifyOwner = notifyOwner;
    }

    @Async
    @EventListener
    public void on(ContactMessageReceived event) {
        try {
            notifyOwner.onMessageReceived(event);
        } catch (RuntimeException failure) {
            // Only the id and the error type are logged: never the visitor's personal data.
            LOG.warn("Could not notify the owner about message {}: {}",
                    event.messageId().value(), failure.getClass().getSimpleName());
        }
    }
}

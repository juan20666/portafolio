package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.notification;

import dev.juanpablocorrea.portfolio.contact.application.port.out.OwnerNotifier;
import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Used while no email channel is configured (local development): leaves a trace in the log. */
public class LogOnlyOwnerNotifier implements OwnerNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(LogOnlyOwnerNotifier.class);

    @Override
    public void notifyNewMessage(ContactMessageReceived event) {
        LOG.info("New contact message {} received (email notification is not configured)",
                event.messageId().value());
    }
}

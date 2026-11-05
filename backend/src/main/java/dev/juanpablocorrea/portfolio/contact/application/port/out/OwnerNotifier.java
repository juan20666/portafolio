package dev.juanpablocorrea.portfolio.contact.application.port.out;

import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;

/** Driven port: reaches the owner by whatever channel is configured (email, log...). */
public interface OwnerNotifier {

    void notifyNewMessage(ContactMessageReceived event);
}

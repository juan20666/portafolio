package dev.juanpablocorrea.portfolio.contact.application.port.in;

import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;

/** Driving port: the owner of the portfolio must hear about a new message. */
public interface NotifyOwnerUseCase {

    void onMessageReceived(ContactMessageReceived event);
}

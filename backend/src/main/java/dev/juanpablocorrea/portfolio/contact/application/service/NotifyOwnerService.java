package dev.juanpablocorrea.portfolio.contact.application.service;

import dev.juanpablocorrea.portfolio.contact.application.port.in.NotifyOwnerUseCase;
import dev.juanpablocorrea.portfolio.contact.application.port.out.OwnerNotifier;
import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;

import java.util.Objects;

/** Reacts to a new message by telling the owner through the configured channel. */
public final class NotifyOwnerService implements NotifyOwnerUseCase {

    private final OwnerNotifier notifier;

    public NotifyOwnerService(OwnerNotifier notifier) {
        this.notifier = Objects.requireNonNull(notifier);
    }

    @Override
    public void onMessageReceived(ContactMessageReceived event) {
        notifier.notifyNewMessage(event);
    }
}

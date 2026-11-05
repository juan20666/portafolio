package dev.juanpablocorrea.portfolio.contact.application.service;

import dev.juanpablocorrea.portfolio.contact.application.command.ReceiveContactMessageCommand;
import dev.juanpablocorrea.portfolio.contact.application.command.Receipt;
import dev.juanpablocorrea.portfolio.contact.application.port.in.ReceiveContactMessageUseCase;
import dev.juanpablocorrea.portfolio.contact.application.port.out.DomainEventPublisher;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;
import dev.juanpablocorrea.portfolio.contact.domain.repository.ContactMessageRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates the "leave a message" use case. It holds no business rules:
 * those live in {@link ContactMessage}. It only decides the order of the steps.
 */
public final class ReceiveContactMessageService implements ReceiveContactMessageUseCase {

    private final ContactMessageRepository repository;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    public ReceiveContactMessageService(
            ContactMessageRepository repository, DomainEventPublisher publisher, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.publisher = Objects.requireNonNull(publisher);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Receipt receive(ReceiveContactMessageCommand command) {
        Instant now = clock.instant();

        if (command.looksLikeBot()) {
            // Looks accepted to the bot, but nothing is stored and nobody is notified.
            return new Receipt(MessageId.generate().value().toString(), now);
        }

        ContactMessage message = ContactMessage.receive(
                command.submissionId(),
                command.name(),
                command.email(),
                command.reason(),
                command.message(),
                command.consent(),
                now);

        Optional<ContactMessage> alreadyStored = repository.findById(message.id());
        if (alreadyStored.isPresent()) {
            // A retried request: answer as before, store and notify nothing twice.
            return receiptOf(alreadyStored.get());
        }

        repository.save(message);
        publisher.publish(message.pullEvents());
        return receiptOf(message);
    }

    private static Receipt receiptOf(ContactMessage message) {
        return new Receipt(message.id().value().toString(), message.receivedAt());
    }
}

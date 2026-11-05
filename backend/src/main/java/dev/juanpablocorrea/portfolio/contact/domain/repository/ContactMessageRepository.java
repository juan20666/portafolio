package dev.juanpablocorrea.portfolio.contact.domain.repository;

import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;

import java.util.Optional;

/** Port: how the domain asks for its aggregates to be stored and found. */
public interface ContactMessageRepository {

    void save(ContactMessage message);

    Optional<ContactMessage> findById(MessageId id);
}

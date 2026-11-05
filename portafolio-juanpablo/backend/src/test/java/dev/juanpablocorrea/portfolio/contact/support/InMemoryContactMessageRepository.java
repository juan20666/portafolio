package dev.juanpablocorrea.portfolio.contact.support;

import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;
import dev.juanpablocorrea.portfolio.contact.domain.repository.ContactMessageRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Hand-written fake of the repository port: no database and no mocking library needed. */
public final class InMemoryContactMessageRepository implements ContactMessageRepository {

    private final Map<MessageId, ContactMessage> stored = new LinkedHashMap<>();
    private int saveCalls;

    @Override
    public void save(ContactMessage message) {
        saveCalls++;
        stored.put(message.id(), message);
    }

    @Override
    public Optional<ContactMessage> findById(MessageId id) {
        return Optional.ofNullable(stored.get(id));
    }

    public int size() {
        return stored.size();
    }

    public int saveCalls() {
        return saveCalls;
    }
}

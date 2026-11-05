package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.persistence;

import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;
import dev.juanpablocorrea.portfolio.contact.domain.repository.ContactMessageRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Driven adapter: implements the domain's repository port on top of PostgreSQL. */
@Component
public class JpaContactMessageRepository implements ContactMessageRepository {

    private final SpringDataContactMessageRepository jpa;

    public JpaContactMessageRepository(SpringDataContactMessageRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ContactMessage message) {
        jpa.save(ContactMessagePersistenceMapper.toEntity(message));
    }

    @Override
    public Optional<ContactMessage> findById(MessageId id) {
        return jpa.findById(id.value()).map(ContactMessagePersistenceMapper::toDomain);
    }
}

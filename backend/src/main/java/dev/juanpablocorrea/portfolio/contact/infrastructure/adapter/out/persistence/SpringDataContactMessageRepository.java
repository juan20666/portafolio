package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data does the plumbing; only the adapter below talks to this interface. */
public interface SpringDataContactMessageRepository extends JpaRepository<ContactMessageJpaEntity, UUID> {
}

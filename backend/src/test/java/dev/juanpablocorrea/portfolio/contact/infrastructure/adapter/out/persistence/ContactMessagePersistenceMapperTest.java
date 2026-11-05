package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.persistence;

import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactReason;
import dev.juanpablocorrea.portfolio.contact.support.ContactFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactMessagePersistenceMapperTest {

    @Test
    void aMessageSurvivesTheTripToTheTableAndBack() {
        ContactMessage original = ContactMessage.receive(
                ContactFixtures.ID, "Ana Pérez", "ana@example.com", "colaboracion",
                "Hola, ¿colaboramos en un proyecto?", true, ContactFixtures.NOW);

        ContactMessageJpaEntity row = ContactMessagePersistenceMapper.toEntity(original);
        ContactMessage restored = ContactMessagePersistenceMapper.toDomain(row);

        assertEquals("colaboracion", row.getReason());
        assertEquals(original, restored);
        assertEquals("Ana Pérez", restored.senderName().value());
        assertEquals(ContactReason.COLLABORATION, restored.reason());
        assertEquals(ContactFixtures.NOW, restored.receivedAt());
        assertTrue(restored.pullEvents().isEmpty());
    }
}

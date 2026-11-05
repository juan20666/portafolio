package dev.juanpablocorrea.portfolio.contact.application;

import dev.juanpablocorrea.portfolio.contact.application.service.NotifyOwnerService;
import dev.juanpablocorrea.portfolio.contact.domain.event.ContactMessageReceived;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.support.ContactFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class NotifyOwnerServiceTest {

    @Test
    void passesTheEventToTheConfiguredChannel() {
        List<ContactMessageReceived> delivered = new ArrayList<>();
        NotifyOwnerService service = new NotifyOwnerService(delivered::add);

        ContactMessage message = ContactMessage.receive(
                ContactFixtures.ID, "Ana Pérez", "ana@example.com", "oferta",
                "Hola, queremos conversar contigo.", true, ContactFixtures.NOW);
        ContactMessageReceived event = (ContactMessageReceived) message.pullEvents().get(0);

        service.onMessageReceived(event);

        assertEquals(1, delivered.size());
        assertSame(event, delivered.get(0));
    }
}

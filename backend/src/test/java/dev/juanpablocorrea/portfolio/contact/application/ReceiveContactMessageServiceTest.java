package dev.juanpablocorrea.portfolio.contact.application;

import dev.juanpablocorrea.portfolio.contact.application.command.ReceiveContactMessageCommand;
import dev.juanpablocorrea.portfolio.contact.application.command.Receipt;
import dev.juanpablocorrea.portfolio.contact.application.service.ReceiveContactMessageService;
import dev.juanpablocorrea.portfolio.contact.domain.error.ValidationFailedException;
import dev.juanpablocorrea.portfolio.contact.support.ContactFixtures;
import dev.juanpablocorrea.portfolio.contact.support.InMemoryContactMessageRepository;
import dev.juanpablocorrea.portfolio.contact.support.RecordingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiveContactMessageServiceTest {

    private InMemoryContactMessageRepository repository;
    private RecordingEventPublisher publisher;
    private ReceiveContactMessageService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryContactMessageRepository();
        publisher = new RecordingEventPublisher();
        service = new ReceiveContactMessageService(repository, publisher, ContactFixtures.fixedClock());
    }

    @Test
    void storesTheMessageAndAnnouncesItOnce() {
        Receipt receipt = service.receive(ContactFixtures.validCommand());

        assertEquals(ContactFixtures.ID, receipt.messageId());
        assertEquals(ContactFixtures.NOW, receipt.receivedAt());
        assertEquals(1, repository.size());
        assertEquals(1, publisher.published().size());
    }

    @Test
    void aRetriedSubmissionReturnsTheSameReceiptWithoutDuplicates() {
        Receipt first = service.receive(ContactFixtures.validCommand());
        Receipt retry = service.receive(ContactFixtures.validCommand());

        assertEquals(first, retry);
        assertEquals(1, repository.saveCalls());
        assertEquals(1, publisher.published().size());
    }

    @Test
    void aDifferentSubmissionIdIsANewMessage() {
        service.receive(ContactFixtures.validCommand());
        service.receive(ContactFixtures.validCommand("8d1e6a2b-0c3f-4b5a-8e7d-9f1a2b3c4d5e"));

        assertEquals(2, repository.size());
        assertEquals(2, publisher.published().size());
    }

    @Test
    void invalidDataStoresAndAnnouncesNothing() {
        ReceiveContactMessageCommand invalid = new ReceiveContactMessageCommand(
                ContactFixtures.ID, "A", "mal", "oferta", "corto", false, "");

        assertThrows(ValidationFailedException.class, () -> service.receive(invalid));
        assertEquals(0, repository.size());
        assertTrue(publisher.published().isEmpty());
    }

    @Test
    void aBotFillingTheHoneypotSeesSuccessButNothingHappens() {
        ReceiveContactMessageCommand bot = new ReceiveContactMessageCommand(
                ContactFixtures.ID, "Bot", "bot@spam.example", "oferta",
                "Compra relojes baratos aquí mismo.", true, "https://spam.example");

        Receipt receipt = service.receive(bot);

        assertEquals(ContactFixtures.NOW, receipt.receivedAt());
        assertEquals(0, repository.size());
        assertTrue(publisher.published().isEmpty());
    }
}

package dev.juanpablocorrea.portfolio.contact.domain;

import dev.juanpablocorrea.portfolio.contact.domain.error.InvalidValueException;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactReason;
import dev.juanpablocorrea.portfolio.contact.domain.model.EmailAddress;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageBody;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;
import dev.juanpablocorrea.portfolio.contact.domain.model.SenderName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueObjectsTest {

    @Test
    void nameIsTrimmedAndItsWhitespaceCollapsed() {
        assertEquals("Ana María Pérez", SenderName.of("  Ana \t María\n Pérez ").value());
    }

    @Test
    void nameLengthIsBounded() {
        assertThrows(InvalidValueException.class, () -> SenderName.of("A"));
        assertThrows(InvalidValueException.class, () -> SenderName.of("x".repeat(121)));
        assertEquals(120, SenderName.of("x".repeat(120)).value().length());
    }

    @Test
    void nameRejectsControlCharacters() {
        assertThrows(InvalidValueException.class, () -> SenderName.of("Ana\u0000Pérez"));
    }

    @Test
    void emailIsLowercased() {
        assertEquals("ana.perez@example.com", EmailAddress.of(" Ana.Perez@Example.COM ").value());
    }

    @Test
    void emailRejectsBadShapesAndHeaderInjection() {
        assertThrows(InvalidValueException.class, () -> EmailAddress.of("sin-arroba.com"));
        assertThrows(InvalidValueException.class, () -> EmailAddress.of("a@b"));
        assertThrows(InvalidValueException.class, () -> EmailAddress.of("a@b.c"));
        assertThrows(InvalidValueException.class, () -> EmailAddress.of("ana@example.com\nBcc: x@y.zz"));
        assertThrows(InvalidValueException.class, () -> EmailAddress.of(null));
    }

    @Test
    void emailLengthIsBounded() {
        String tooLong = "a".repeat(250) + "@b.co";
        assertThrows(InvalidValueException.class, () -> EmailAddress.of(tooLong));
    }

    @Test
    void bodyKeepsLineBreaksAsLfAndRejectsOtherControlCharacters() {
        assertEquals("Línea uno\nLínea dos", MessageBody.of("Línea uno\r\nLínea dos\r\n").value());
        assertThrows(InvalidValueException.class, () -> MessageBody.of("Mensaje con \u0007 campana."));
    }

    @Test
    void bodyLengthIsBoundedAfterTrimming() {
        assertThrows(InvalidValueException.class, () -> MessageBody.of("   corto   "));
        assertThrows(InvalidValueException.class, () -> MessageBody.of("x".repeat(2001)));
        assertEquals(2000, MessageBody.of("x".repeat(2000)).value().length());
    }

    @Test
    void reasonIsFoundByItsCodeIgnoringCase() {
        assertEquals(ContactReason.COLLABORATION, ContactReason.fromCode(" Colaboracion "));
        assertThrows(InvalidValueException.class, () -> ContactReason.fromCode("spam"));
        assertThrows(InvalidValueException.class, () -> ContactReason.fromCode(null));
    }

    @Test
    void idMustBeACanonicalUuid() {
        assertEquals("3f2b8c1e-5d4a-4e7b-9a10-2c6d8e0f1a23",
                MessageId.parse(" 3F2B8C1E-5D4A-4E7B-9A10-2C6D8E0F1A23 ").value().toString());
        assertThrows(InvalidValueException.class, () -> MessageId.parse("1-1-1-1-1"));
        assertThrows(InvalidValueException.class, () -> MessageId.parse(null));
    }
}

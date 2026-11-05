package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientAddressTest {

    @Test
    void usesTheConnectionAddressWhenNoProxyIsTrusted() {
        assertEquals("10.0.0.9", ClientAddress.resolve("1.2.3.4", "10.0.0.9", 0));
    }

    @Test
    void usesTheLastEntryWhenOneProxyIsTrusted() {
        assertEquals("203.0.113.7", ClientAddress.resolve("6.6.6.6, 203.0.113.7", "10.0.0.9", 1));
    }

    @Test
    void ignoresEntriesForgedByTheClientOnTheLeft() {
        assertEquals("203.0.113.7", ClientAddress.resolve("1.1.1.1, 2.2.2.2, 203.0.113.7", "10.0.0.9", 1));
    }

    @Test
    void countsHopsFromTheRight() {
        assertEquals("198.51.100.4", ClientAddress.resolve("198.51.100.4, 203.0.113.7", "10.0.0.9", 2));
    }

    @Test
    void neverGoesPastTheStartOfTheHeader() {
        assertEquals("198.51.100.4", ClientAddress.resolve("198.51.100.4", "10.0.0.9", 5));
    }

    @Test
    void fallsBackToTheConnectionAddress() {
        assertEquals("10.0.0.9", ClientAddress.resolve(null, "10.0.0.9", 1));
        assertEquals("10.0.0.9", ClientAddress.resolve("  ", "10.0.0.9", 1));
        assertEquals("unknown", ClientAddress.resolve(null, null, 1));
    }
}

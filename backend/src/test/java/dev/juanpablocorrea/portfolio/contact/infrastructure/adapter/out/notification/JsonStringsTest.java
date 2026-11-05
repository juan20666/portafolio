package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonStringsTest {

    @Test
    void wrapsPlainTextInQuotes() {
        assertEquals("\"Hola Pérez ñ 😀\"", JsonStrings.quote("Hola Pérez ñ 😀"));
    }

    @Test
    void escapesQuotesBackslashesAndLineBreaks() {
        assertEquals("\"a\\\"b\\\\c\\nd\\te\"", JsonStrings.quote("a\"b\\c\nd\te"));
    }

    @Test
    void escapesOtherControlCharactersAsUnicode() {
        assertEquals("\"\\u0001\\u001f\"", JsonStrings.quote("\u0001\u001f"));
    }
}

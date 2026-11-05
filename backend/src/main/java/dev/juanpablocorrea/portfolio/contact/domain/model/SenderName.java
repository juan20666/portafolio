package dev.juanpablocorrea.portfolio.contact.domain.model;

import dev.juanpablocorrea.portfolio.contact.domain.error.InvalidValueException;

/** Name the visitor gives. Whitespace is normalised so "  Ana   Pérez " becomes "Ana Pérez". */
public record SenderName(String value) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 120;

    public SenderName {
        String normalised = Text.collapseWhitespace(value);
        if (normalised.length() < MIN_LENGTH
                || normalised.length() > MAX_LENGTH
                || Text.hasControlCharacters(normalised, "")) {
            throw new InvalidValueException(
                    "Escribe tu nombre (entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres).");
        }
        value = normalised;
    }

    public static SenderName of(String raw) {
        return new SenderName(raw);
    }
}

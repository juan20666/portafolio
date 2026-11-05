package dev.juanpablocorrea.portfolio.contact.domain.model;

import dev.juanpablocorrea.portfolio.contact.domain.error.InvalidValueException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email address of the visitor. The pattern is deliberately simple (the only real proof
 * of an address is writing back to it) and forbids whitespace, which also blocks
 * header injection when the address is used as a Reply-To.
 */
public record EmailAddress(String value) {

    public static final int MAX_LENGTH = 254;

    private static final Pattern SHAPE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    public EmailAddress {
        String normalised = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (normalised.length() > MAX_LENGTH
                || !SHAPE.matcher(normalised).matches()
                || Text.hasControlCharacters(normalised, "")) {
            throw new InvalidValueException("Escribe un correo válido.");
        }
        value = normalised;
    }

    public static EmailAddress of(String raw) {
        return new EmailAddress(raw);
    }
}

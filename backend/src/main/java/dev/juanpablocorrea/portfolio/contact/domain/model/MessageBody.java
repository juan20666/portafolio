package dev.juanpablocorrea.portfolio.contact.domain.model;

import dev.juanpablocorrea.portfolio.contact.domain.error.InvalidValueException;

/** The text of the message. Line breaks are kept (as LF); other control characters are rejected. */
public record MessageBody(String value) {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 2000;

    public MessageBody {
        String normalised = Text.nullToEmpty(value).replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalised.length() < MIN_LENGTH
                || normalised.length() > MAX_LENGTH
                || Text.hasControlCharacters(normalised, "\n\t")) {
            throw new InvalidValueException(
                    "El mensaje debe tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres.");
        }
        value = normalised;
    }

    public static MessageBody of(String raw) {
        return new MessageBody(raw);
    }
}

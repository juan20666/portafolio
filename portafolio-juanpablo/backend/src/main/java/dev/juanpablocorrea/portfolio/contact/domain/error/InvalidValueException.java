package dev.juanpablocorrea.portfolio.contact.domain.error;

/** A single value object rejected the raw input it was given. */
public final class InvalidValueException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidValueException(String message) {
        super(message);
    }
}

package dev.juanpablocorrea.portfolio.contact.domain.error;

/**
 * Base type for every business-rule violation raised by the domain.
 * Messages are written for the end user, so adapters can show them as they are.
 */
public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }
}

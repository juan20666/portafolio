package dev.juanpablocorrea.portfolio.contact.domain.model;

import dev.juanpablocorrea.portfolio.contact.domain.error.InvalidValueException;
import dev.juanpablocorrea.portfolio.contact.domain.error.ValidationFailedException;
import dev.juanpablocorrea.portfolio.contact.domain.error.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Collects every broken rule instead of stopping at the first one,
 * so the visitor can fix the whole form in a single round trip.
 */
final class Violations {

    private final List<Violation> found = new ArrayList<>();

    /** Builds a value object; a rejection is recorded and {@code null} is returned. */
    <T> T collect(String field, Supplier<T> factory) {
        try {
            return factory.get();
        } catch (InvalidValueException rejected) {
            found.add(new Violation(field, rejected.getMessage()));
            return null;
        }
    }

    void require(boolean condition, String field, String message) {
        if (!condition) {
            found.add(new Violation(field, message));
        }
    }

    void throwIfAny() {
        if (!found.isEmpty()) {
            throw new ValidationFailedException(found);
        }
    }
}

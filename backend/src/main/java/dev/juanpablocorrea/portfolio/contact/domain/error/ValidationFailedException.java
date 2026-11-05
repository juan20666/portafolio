package dev.juanpablocorrea.portfolio.contact.domain.error;

import java.util.List;

/** Every rule broken by one attempt to build an aggregate, reported together. */
public final class ValidationFailedException extends DomainException {

    private static final long serialVersionUID = 1L;

    private final transient List<Violation> violations;

    public ValidationFailedException(List<Violation> violations) {
        super("Hay datos por corregir.");
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}

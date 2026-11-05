package dev.juanpablocorrea.portfolio.contact.domain.error;

/** One broken rule, tied to the input field that caused it. */
public record Violation(String field, String message) {
}

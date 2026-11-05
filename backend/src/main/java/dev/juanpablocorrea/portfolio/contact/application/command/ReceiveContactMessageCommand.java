package dev.juanpablocorrea.portfolio.contact.application.command;

/**
 * What the outside world asks for. Every field is the raw text the visitor sent;
 * the domain is the one that decides whether it is acceptable.
 *
 * @param honeypot hidden form field that people leave empty and simple bots fill in
 */
public record ReceiveContactMessageCommand(
        String submissionId,
        String name,
        String email,
        String reason,
        String message,
        boolean consent,
        String honeypot) {

    public boolean looksLikeBot() {
        return honeypot != null && !honeypot.isBlank();
    }
}

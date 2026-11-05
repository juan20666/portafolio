package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.dto;

import dev.juanpablocorrea.portfolio.contact.application.command.ReceiveContactMessageCommand;

/** JSON body sent by the portfolio form. */
public record ContactMessageRequest(
        String submissionId,
        String name,
        String email,
        String reason,
        String message,
        Boolean consent,
        String website) {

    public ReceiveContactMessageCommand toCommand() {
        return new ReceiveContactMessageCommand(
                submissionId, name, email, reason, message, Boolean.TRUE.equals(consent), website);
    }
}

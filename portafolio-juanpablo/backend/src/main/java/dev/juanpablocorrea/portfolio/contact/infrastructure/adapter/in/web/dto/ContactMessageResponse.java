package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.dto;

import dev.juanpablocorrea.portfolio.contact.application.command.Receipt;

/** JSON answer to an accepted message. */
public record ContactMessageResponse(String id, String receivedAt) {

    public static ContactMessageResponse from(Receipt receipt) {
        return new ContactMessageResponse(receipt.messageId(), receipt.receivedAt().toString());
    }
}

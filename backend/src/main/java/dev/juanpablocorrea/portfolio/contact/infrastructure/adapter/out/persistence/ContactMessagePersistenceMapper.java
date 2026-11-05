package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.persistence;

import dev.juanpablocorrea.portfolio.contact.domain.model.ContactMessage;
import dev.juanpablocorrea.portfolio.contact.domain.model.ContactReason;
import dev.juanpablocorrea.portfolio.contact.domain.model.EmailAddress;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageBody;
import dev.juanpablocorrea.portfolio.contact.domain.model.MessageId;
import dev.juanpablocorrea.portfolio.contact.domain.model.SenderName;

/** Converts between the aggregate and its table row. */
final class ContactMessagePersistenceMapper {

    private ContactMessagePersistenceMapper() {
    }

    static ContactMessageJpaEntity toEntity(ContactMessage message) {
        return new ContactMessageJpaEntity(
                message.id().value(),
                message.senderName().value(),
                message.senderEmail().value(),
                message.reason().code(),
                message.body().value(),
                message.receivedAt());
    }

    static ContactMessage toDomain(ContactMessageJpaEntity row) {
        return ContactMessage.restore(
                new MessageId(row.getId()),
                SenderName.of(row.getSenderName()),
                EmailAddress.of(row.getSenderEmail()),
                ContactReason.fromCode(row.getReason()),
                MessageBody.of(row.getBody()),
                row.getReceivedAt());
    }
}

package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Database shape of a contact message. It is a persistence detail: the domain never sees it,
 * and the mapper is the only code that converts between the two.
 */
@Entity
@Table(name = "contact_messages")
public class ContactMessageJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sender_name", nullable = false, length = 120)
    private String senderName;

    @Column(name = "sender_email", nullable = false, length = 254)
    private String senderEmail;

    @Column(name = "reason", nullable = false, length = 20)
    private String reason;

    @Column(name = "body", nullable = false, length = 2000)
    private String body;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /** Required by JPA. */
    protected ContactMessageJpaEntity() {
    }

    public ContactMessageJpaEntity(
            UUID id, String senderName, String senderEmail, String reason, String body, Instant receivedAt) {
        this.id = id;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.reason = reason;
        this.body = body;
        this.receivedAt = receivedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getReason() {
        return reason;
    }

    public String getBody() {
        return body;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}

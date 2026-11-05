package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.out.notification;

/** The notification channel did not accept the message. The text never contains personal data. */
public class NotificationFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotificationFailedException(String message) {
        super(message);
    }

    public NotificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

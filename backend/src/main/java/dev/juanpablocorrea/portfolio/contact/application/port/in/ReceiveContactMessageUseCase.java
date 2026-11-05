package dev.juanpablocorrea.portfolio.contact.application.port.in;

import dev.juanpablocorrea.portfolio.contact.application.command.ReceiveContactMessageCommand;
import dev.juanpablocorrea.portfolio.contact.application.command.Receipt;

/** Driving port: a visitor leaves a message. */
public interface ReceiveContactMessageUseCase {

    /**
     * @throws dev.juanpablocorrea.portfolio.contact.domain.error.ValidationFailedException
     *         when the data breaks any rule of the domain
     */
    Receipt receive(ReceiveContactMessageCommand command);
}

package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web;

import dev.juanpablocorrea.portfolio.contact.application.command.Receipt;
import dev.juanpablocorrea.portfolio.contact.application.port.in.ReceiveContactMessageUseCase;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.dto.ApiProblem;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.dto.ContactMessageRequest;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.dto.ContactMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Driving adapter: turns an HTTP request into a call to the use case. */
@RestController
@RequestMapping("/api/v1/contact-messages")
@Tag(name = "Contacto", description = "Mensajes que dejan los visitantes del portafolio")
public class ContactMessageController {

    private final ReceiveContactMessageUseCase receiveContactMessage;

    public ContactMessageController(ReceiveContactMessageUseCase receiveContactMessage) {
        this.receiveContactMessage = receiveContactMessage;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Deja un mensaje",
            description = "Es idempotente: reenviar el mismo submissionId devuelve el recibo original sin duplicar.")
    @ApiResponse(responseCode = "201", description = "Mensaje recibido")
    @ApiResponse(
            responseCode = "422",
            description = "Hay datos por corregir (la lista `errors` indica cuáles)",
            content = @Content(schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "429", description = "Demasiados envíos seguidos desde la misma dirección")
    public ResponseEntity<ContactMessageResponse> receive(@RequestBody ContactMessageRequest request) {
        Receipt receipt = receiveContactMessage.receive(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactMessageResponse.from(receipt));
    }
}

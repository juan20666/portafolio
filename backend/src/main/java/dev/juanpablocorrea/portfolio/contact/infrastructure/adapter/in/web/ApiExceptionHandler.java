package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web;

import dev.juanpablocorrea.portfolio.contact.domain.error.ValidationFailedException;
import dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.dto.ApiProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Translates failures into one consistent JSON error shape. Internal details never leave the server. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<ApiProblem> onValidationFailed(ValidationFailedException failure) {
        List<ApiProblem.FieldProblem> errors = failure.violations().stream()
                .map(violation -> new ApiProblem.FieldProblem(violation.field(), violation.message()))
                .toList();
        ApiProblem problem = new ApiProblem(
                "about:blank", "Datos no válidos", HttpStatus.UNPROCESSABLE_ENTITY.value(),
                failure.getMessage(), errors);
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiProblem> onUnreadableBody(HttpMessageNotReadableException failure) {
        return problemResponse(HttpStatus.BAD_REQUEST.value(), ApiProblem.of(
                HttpStatus.BAD_REQUEST.value(), "Solicitud no válida", "El cuerpo debe ser un JSON válido."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> onAnythingElse(Exception failure) {
        if (failure instanceof ErrorResponse standard) {
            // 404, 405, 415... raised by Spring MVC itself: keep their status, hide their details.
            int status = standard.getStatusCode().value();
            return ResponseEntity.status(status)
                    .headers(standard.getHeaders())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(ApiProblem.of(status, "Solicitud no válida", "La solicitud no se puede atender."));
        }
        LOG.error("Unexpected failure while handling a request", failure);
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ApiProblem.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error del servidor",
                "Algo salió mal de nuestro lado. Inténtalo de nuevo en un momento."));
    }

    private static ResponseEntity<ApiProblem> problemResponse(int status, ApiProblem problem) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}

package dev.juanpablocorrea.portfolio.contact.domain.model;

import dev.juanpablocorrea.portfolio.contact.domain.error.InvalidValueException;

import java.util.Arrays;
import java.util.Locale;

/** Why the visitor is writing. The {@code code} is the value used on the wire and in the database. */
public enum ContactReason {

    JOB_OFFER("oferta", "Oferta laboral o práctica"),
    COLLABORATION("colaboracion", "Colaboración en un proyecto"),
    QUESTION("consulta", "Pregunta sobre mi trabajo"),
    OTHER("otro", "Otro motivo");

    private final String code;
    private final String label;

    ContactReason(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static ContactReason fromCode(String raw) {
        String candidate = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(reason -> reason.code.equals(candidate))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("Elige un motivo de contacto."));
    }
}

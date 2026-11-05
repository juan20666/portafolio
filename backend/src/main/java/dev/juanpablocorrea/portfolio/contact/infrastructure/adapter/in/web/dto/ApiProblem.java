package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.dto;

import java.util.List;

/** Error body, shaped after RFC 9457 "problem details" plus a list of field errors. */
public record ApiProblem(String type, String title, int status, String detail, List<FieldProblem> errors) {

    /** One field of the request that must be fixed. */
    public record FieldProblem(String field, String message) {
    }

    public static ApiProblem of(int status, String title, String detail) {
        return new ApiProblem("about:blank", title, status, detail, List.of());
    }
}

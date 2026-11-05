package dev.juanpablocorrea.portfolio.contact.support;

import dev.juanpablocorrea.portfolio.contact.application.command.ReceiveContactMessageCommand;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Ready-made valid data so each test only spells out what it changes. */
public final class ContactFixtures {

    public static final String ID = "3f2b8c1e-5d4a-4e7b-9a10-2c6d8e0f1a23";
    public static final Instant NOW = Instant.parse("2026-11-03T11:00:00Z");

    private ContactFixtures() {
    }

    public static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static ReceiveContactMessageCommand validCommand() {
        return validCommand(ID);
    }

    public static ReceiveContactMessageCommand validCommand(String submissionId) {
        return new ReceiveContactMessageCommand(
                submissionId,
                "  Ana   María  Pérez ",
                "Ana.Perez@Example.com",
                "oferta",
                "Hola Juan Pablo, vimos tu portafolio y nos gustaría hablar contigo.",
                true,
                "");
    }

    public static String validJson(String submissionId) {
        return "{\"submissionId\":\"" + submissionId + "\",\"name\":\"Ana Pérez\","
                + "\"email\":\"ana@example.com\",\"reason\":\"oferta\","
                + "\"message\":\"Hola Juan Pablo, vimos tu portafolio y queremos hablar.\","
                + "\"consent\":true,\"website\":\"\"}";
    }
}

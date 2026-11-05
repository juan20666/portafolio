package dev.juanpablocorrea.portfolio.contact.domain.model;

/** Small text helpers shared by the value objects of this package. */
final class Text {

    private Text() {
    }

    static String nullToEmpty(String raw) {
        return raw == null ? "" : raw;
    }

    /** Trims and collapses any run of whitespace (including line breaks) into one space. */
    static String collapseWhitespace(String raw) {
        return nullToEmpty(raw).strip().replaceAll("\\s+", " ");
    }

    /** True when the text holds control characters other than the ones listed in {@code allowed}. */
    static boolean hasControlCharacters(String text, String allowed) {
        return text.chars().anyMatch(c -> Character.isISOControl(c) && allowed.indexOf(c) < 0);
    }
}

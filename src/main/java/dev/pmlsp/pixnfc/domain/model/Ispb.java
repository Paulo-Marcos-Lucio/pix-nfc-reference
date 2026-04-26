package dev.pmlsp.pixnfc.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ISPB — Identificador do Sistema de Pagamentos Brasileiro.
 * Always 8 numeric characters, identifies the participant institution.
 */
public record Ispb(String value) {

    private static final Pattern PATTERN = Pattern.compile("\\d{8}");

    public Ispb {
        Objects.requireNonNull(value, "value");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid ISPB (expected 8 digits): " + value);
        }
    }

    public static Ispb of(String value) {
        return new Ispb(value);
    }
}

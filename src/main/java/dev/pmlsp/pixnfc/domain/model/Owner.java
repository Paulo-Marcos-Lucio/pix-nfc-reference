package dev.pmlsp.pixnfc.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record Owner(OwnerType type, String document, String name, String tradeName) {

    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CNPJ = Pattern.compile("\\d{14}");
    private static final int MAX_NAME = 100;
    private static final int MAX_TRADE_NAME = 100;

    public Owner {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(name, "name");
        switch (type) {
            case NATURAL_PERSON -> {
                if (!CPF.matcher(document).matches()) {
                    throw new IllegalArgumentException("NATURAL_PERSON requires 11-digit CPF");
                }
            }
            case LEGAL_PERSON -> {
                if (!CNPJ.matcher(document).matches()) {
                    throw new IllegalArgumentException("LEGAL_PERSON requires 14-digit CNPJ");
                }
            }
        }
        if (name.isBlank() || name.length() > MAX_NAME) {
            throw new IllegalArgumentException("name must be 1-%d chars".formatted(MAX_NAME));
        }
        if (tradeName != null && tradeName.length() > MAX_TRADE_NAME) {
            throw new IllegalArgumentException("tradeName must be at most %d chars".formatted(MAX_TRADE_NAME));
        }
    }

    public static Owner naturalPerson(String cpf, String name) {
        return new Owner(OwnerType.NATURAL_PERSON, cpf, name, null);
    }

    public static Owner legalPerson(String cnpj, String name, String tradeName) {
        return new Owner(OwnerType.LEGAL_PERSON, cnpj, name, tradeName);
    }

    public Optional<String> tradeNameOpt() {
        return Optional.ofNullable(tradeName);
    }

    /** Mask document for log/audit output. */
    public String maskedDocument() {
        if (document == null || document.length() < 4) return "***";
        return document.substring(0, 2) + "***" + document.substring(document.length() - 2);
    }
}

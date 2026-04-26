package dev.pmlsp.pixnfc.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record PixKey(PixKeyType type, String value) {

    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CNPJ = Pattern.compile("\\d{14}");
    private static final Pattern PHONE = Pattern.compile("\\+\\d{12,14}");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern EVP = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public PixKey {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        boolean ok = switch (type) {
            case CPF -> CPF.matcher(value).matches();
            case CNPJ -> CNPJ.matcher(value).matches();
            case PHONE -> PHONE.matcher(value).matches();
            case EMAIL -> EMAIL.matcher(value).matches();
            case EVP -> EVP.matcher(value).matches();
        };
        if (!ok) {
            throw new IllegalArgumentException("invalid %s key: %s".formatted(type, mask(value)));
        }
    }

    public static PixKey of(PixKeyType type, String value) {
        return new PixKey(type, value);
    }

    /**
     * Auto-detect key type from format and parse. Used in config binding
     * where the operator types just the key string.
     */
    public static PixKey parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        String value = raw.trim();
        if (CPF.matcher(value).matches()) return new PixKey(PixKeyType.CPF, value);
        if (CNPJ.matcher(value).matches()) return new PixKey(PixKeyType.CNPJ, value);
        if (PHONE.matcher(value).matches()) return new PixKey(PixKeyType.PHONE, value);
        if (EVP.matcher(value).matches()) return new PixKey(PixKeyType.EVP, value);
        if (EMAIL.matcher(value).matches()) return new PixKey(PixKeyType.EMAIL, value);
        throw new IllegalArgumentException("could not infer Pix key type from value");
    }

    /**
     * PII-safe representation for logs, audit and error messages.
     * Never use {@link #value()} directly in observability outputs.
     */
    public String masked() {
        return mask(value);
    }

    private static String mask(String value) {
        if (value == null || value.length() <= 4) return "***";
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}

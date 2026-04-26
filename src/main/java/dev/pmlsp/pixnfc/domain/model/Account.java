package dev.pmlsp.pixnfc.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record Account(Ispb ispb, String branch, String number, AccountType type) {

    private static final Pattern BRANCH = Pattern.compile("\\d{1,4}");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,20}");

    public Account {
        Objects.requireNonNull(ispb, "ispb");
        Objects.requireNonNull(branch, "branch");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(type, "type");
        if (!BRANCH.matcher(branch).matches()) {
            throw new IllegalArgumentException("branch must be 1-4 numeric chars");
        }
        if (!NUMBER.matcher(number).matches()) {
            throw new IllegalArgumentException("account number must be 1-20 numeric chars");
        }
    }
}

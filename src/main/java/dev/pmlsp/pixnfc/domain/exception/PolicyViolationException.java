package dev.pmlsp.pixnfc.domain.exception;

/**
 * The DICT rejected the request for a regulatory/policy reason
 * (e.g. invalid ISPB, owner mismatch, claim already in progress).
 */
public class PolicyViolationException extends DictException {

    private final String code;

    public PolicyViolationException(String code, String message) {
        super("DICT policy violation [%s]: %s".formatted(code, message));
        this.code = code;
    }

    public String code() {
        return code;
    }
}

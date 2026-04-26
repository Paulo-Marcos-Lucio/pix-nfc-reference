package dev.pmlsp.pixnfc.domain.exception;

/**
 * DICT is temporarily unreachable, slow, or returning 5xx — caller may retry
 * after backoff. Mapped to circuit breaker / retry policies in the infrastructure layer.
 */
public class DictUnavailableException extends DictException {

    public DictUnavailableException(String message) {
        super(message);
    }

    public DictUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

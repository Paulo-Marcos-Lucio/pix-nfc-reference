package dev.pmlsp.pixnfc.domain.exception;

import java.time.Duration;
import java.util.Optional;

public class RateLimitedException extends DictException {

    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter) {
        super("DICT rate limit exceeded; retry after " + retryAfter);
        this.retryAfter = retryAfter;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}

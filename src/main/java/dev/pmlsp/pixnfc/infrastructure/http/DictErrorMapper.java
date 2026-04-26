package dev.pmlsp.pixnfc.infrastructure.http;

import dev.pmlsp.pixnfc.domain.exception.DictException;
import dev.pmlsp.pixnfc.domain.exception.KeyNotFoundException;
import dev.pmlsp.pixnfc.domain.exception.PolicyViolationException;
import dev.pmlsp.pixnfc.domain.exception.RateLimitedException;
import dev.pmlsp.pixnfc.domain.exception.SpiUnavailableException;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.infrastructure.http.dto.HttpDtos.ProblemPayload;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

import java.time.Duration;

/**
 * Mapeia falhas HTTP do DICT para a taxonomia de exceções do domain.
 *
 * <ul>
 *   <li>{@code 404 NOT_FOUND} → {@link KeyNotFoundException}</li>
 *   <li>{@code 422 / 400} → {@link PolicyViolationException}</li>
 *   <li>{@code 429 TOO_MANY_REQUESTS} → {@link RateLimitedException}</li>
 *   <li>5xx → {@link SpiUnavailableException} (mesmo nome cobre ambos DICT e SPI)</li>
 * </ul>
 */
public final class DictErrorMapper {

    private DictErrorMapper() {}

    public static DictException toDomain(
            HttpStatusCode status,
            HttpHeaders headers,
            ProblemPayload problem,
            PixKey associatedKey) {

        if (status.is5xxServerError()) {
            return new SpiUnavailableException("DICT %s: %s".formatted(status, safeMessage(problem)));
        }
        if (status.value() == 429) {
            return new RateLimitedException(parseRetryAfter(headers));
        }
        if (status.value() == 404) {
            return associatedKey != null
                    ? new KeyNotFoundException(associatedKey)
                    : new PolicyViolationException("NOT_FOUND", safeMessage(problem));
        }
        if (status.value() == 422 || status.value() == 400) {
            return new PolicyViolationException(safeCode(problem), safeMessage(problem));
        }
        return new PolicyViolationException(
                problem == null ? status.toString() : safeCode(problem),
                safeMessage(problem));
    }

    private static String safeCode(ProblemPayload problem) {
        return problem == null || problem.code() == null ? "UNKNOWN" : problem.code();
    }

    private static String safeMessage(ProblemPayload problem) {
        return problem == null || problem.message() == null ? "(no detail)" : problem.message();
    }

    private static Duration parseRetryAfter(HttpHeaders headers) {
        String header = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (header == null || header.isBlank()) {
            return Duration.ofSeconds(1);
        }
        try {
            return Duration.ofSeconds(Long.parseLong(header.trim()));
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(1);
        }
    }
}

package dev.pmlsp.pixnfc.infrastructure.http;

import dev.pmlsp.pixnfc.domain.exception.DictException;
import dev.pmlsp.pixnfc.domain.exception.SpiUnavailableException;
import dev.pmlsp.pixnfc.domain.model.Account;
import dev.pmlsp.pixnfc.domain.model.AccountType;
import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.Owner;
import dev.pmlsp.pixnfc.domain.model.OwnerType;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.model.PixKeyType;
import dev.pmlsp.pixnfc.domain.port.out.DictGateway;
import dev.pmlsp.pixnfc.infrastructure.http.dto.HttpDtos;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Optional;

/**
 * Resolve uma chave Pix declarada num payload NFC consultando o DICT do BCB
 * (ou o simulator local). Resilience4j {@code dict-lookup} envolve a chamada
 * com circuit breaker, rate limiter e retry — esse é o único uso de DICT
 * neste projeto. Operações completas de DICT vivem em {@code dict-client-reference}.
 */
@Slf4j
public class DictHttpGateway implements DictGateway {

    private static final String INSTANCE = "dict-lookup";

    private final RestClient http;

    public DictHttpGateway(RestClient http) {
        this.http = http;
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @RateLimiter(name = INSTANCE)
    @Retry(name = INSTANCE)
    public Optional<DictEntry> lookup(PixKey key, Ispb requesterIspb) {
        try {
            HttpDtos.EntryPayload payload = http.get()
                    .uri("/entries/{type}/{value}", key.type().name(), key.value())
                    .header("X-Payer-Ispb", requesterIspb.value())
                    .retrieve()
                    .body(HttpDtos.EntryPayload.class);
            return Optional.ofNullable(payload).map(DictHttpGateway::toDomain);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw mapHttpError(ex, key);
        } catch (ResourceAccessException ex) {
            throw new SpiUnavailableException("DICT unreachable: " + ex.getMessage(), ex);
        }
    }

    private static DictException mapHttpError(RestClientResponseException ex, PixKey key) {
        HttpStatusCode status = ex.getStatusCode();
        HttpDtos.ProblemPayload problem = parseProblem(ex);
        log.warn("dict.http.error status={} bodyRaw={} key={}",
                status.value(), ex.getResponseBodyAsString(),
                key == null ? null : key.masked());
        return DictErrorMapper.toDomain(status, ex.getResponseHeaders(), problem, key);
    }

    private static HttpDtos.ProblemPayload parseProblem(RestClientResponseException ex) {
        try {
            return ex.getResponseBodyAs(HttpDtos.ProblemPayload.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static DictEntry toDomain(HttpDtos.EntryPayload p) {
        return DictEntry.builder()
                .key(new PixKey(PixKeyType.valueOf(p.key().type()), p.key().value()))
                .account(new Account(Ispb.of(p.account().ispb()), p.account().branch(),
                        p.account().number(), AccountType.valueOf(p.account().type())))
                .owner(new Owner(OwnerType.valueOf(p.owner().type()), p.owner().document(),
                        p.owner().name(), p.owner().tradeName()))
                .keyOwnershipDate(p.keyOwnershipDate() == null ? Instant.now() : p.keyOwnershipDate())
                .creationDate(p.createdAt() == null ? Instant.now() : p.createdAt())
                .build();
    }
}

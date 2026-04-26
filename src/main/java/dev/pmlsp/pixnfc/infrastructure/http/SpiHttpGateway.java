package dev.pmlsp.pixnfc.infrastructure.http;

import dev.pmlsp.pixnfc.domain.exception.SpiUnavailableException;
import dev.pmlsp.pixnfc.domain.model.Account;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;
import dev.pmlsp.pixnfc.domain.model.PixOperationResult;
import dev.pmlsp.pixnfc.domain.model.Reason;
import dev.pmlsp.pixnfc.domain.port.out.PixSpiGateway;
import dev.pmlsp.pixnfc.infrastructure.http.dto.HttpDtos;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

/**
 * Cliente HTTP/JSON pro SPI (real ou simulator). Resilience4j {@code spi-pay}
 * envolve a chamada com circuit breaker e retry conservador — Pix por NFC tem
 * baixa tolerância a duplicação, retry agressivo causaria cobrança duplicada.
 *
 * <p>Em produção real, esta classe seria substituída por adapter ISO 20022
 * com mensageria pacs.008 — JSON aqui é simplificação didática do reference.
 */
@Slf4j
public class SpiHttpGateway implements PixSpiGateway {

    private static final String INSTANCE = "spi-pay";

    private final RestClient http;

    public SpiHttpGateway(RestClient http) {
        this.http = http;
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public PixOperationResult settle(NfcPayload payload, Account payerAccount) {
        try {
            HttpDtos.SpiSettleRequest body = new HttpDtos.SpiSettleRequest(
                    payload.getChargeId(),
                    payload.getMerchantIspb().value(),
                    new HttpDtos.KeyPayload(
                            payload.getMerchantKey().type().name(),
                            payload.getMerchantKey().value()),
                    payload.getAmountCents(),
                    new HttpDtos.AccountPayload(
                            payerAccount.ispb().value(),
                            payerAccount.branch(),
                            payerAccount.number(),
                            payerAccount.type().name()),
                    payload.getTerminalId());

            HttpDtos.SpiSettleResponse resp = http.post()
                    .uri("/payments/nfc")
                    .body(body)
                    .retrieve()
                    .body(HttpDtos.SpiSettleResponse.class);

            if (resp == null) {
                return PixOperationResult.builder()
                        .settled(false)
                        .failureReason(Reason.UNKNOWN)
                        .build();
            }

            return PixOperationResult.builder()
                    .settled(resp.settled())
                    .endToEndId(resp.endToEndId())
                    .settledAt(resp.settledAt() == null ? Instant.now() : resp.settledAt())
                    .failureReason(resp.failureReason() == null ? null : Reason.valueOf(resp.failureReason()))
                    .build();
        } catch (RestClientResponseException ex) {
            log.warn("spi.http.error status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            if (ex.getStatusCode().is5xxServerError()) {
                throw new SpiUnavailableException("SPI " + ex.getStatusCode(), ex);
            }
            return PixOperationResult.builder()
                    .settled(false)
                    .failureReason(Reason.POLICY_VIOLATION)
                    .build();
        } catch (ResourceAccessException ex) {
            throw new SpiUnavailableException("SPI unreachable: " + ex.getMessage(), ex);
        }
    }
}

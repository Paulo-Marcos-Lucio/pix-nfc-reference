package dev.pmlsp.pixnfc.integration;

import dev.pmlsp.pixnfc.adapter.web.dto.WebDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o fluxo completo end-to-end via HTTP — issue → validate → pay —
 * batendo no simulator embutido (DICT + SPI).
 *
 * <p>Usa {@link RestClient} (cliente HTTP padrão do Spring) apontando para a porta
 * fixa que a app bindou. O Spring Boot 4 removeu o {@code TestRestTemplate}; o
 * {@code RestClient} cobre o mesmo cenário sem dependência de test-support extra.
 */
class NfcEndToEndIT extends AbstractIntegrationIT {

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port())
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .build();
    }

    @Test
    void fullFlowIssueValidateAndPay() {
        WebDtos.IssueChargeRequest issueReq = new WebDtos.IssueChargeRequest(
                "T-IT-1", 250L, "Padaria · IT", 120);
        ResponseEntity<WebDtos.IssueChargeResponse> issueResp = http.post()
                .uri("/v1/nfc/charges")
                .body(issueReq)
                .retrieve()
                .toEntity(WebDtos.IssueChargeResponse.class);
        assertThat(issueResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(issueResp.getBody()).isNotNull();
        String wire = issueResp.getBody().payloadWire();
        assertThat(wire).isNotBlank();

        WebDtos.ValidatePayloadRequest validateReq = new WebDtos.ValidatePayloadRequest(wire);
        ResponseEntity<WebDtos.ValidatePayloadResponse> validateResp = http.post()
                .uri("/v1/nfc/payments/validate")
                .body(validateReq)
                .retrieve()
                .toEntity(WebDtos.ValidatePayloadResponse.class);
        assertThat(validateResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(validateResp.getBody().amountCents()).isEqualTo(250L);

        WebDtos.PayRequest payReq = new WebDtos.PayRequest(
                wire,
                new WebDtos.AccountDto("99988877", "0001", "1234567", "CACC"),
                "device-it-1");
        ResponseEntity<WebDtos.PayResponse> payResp = http.post()
                .uri("/v1/nfc/payments")
                .body(payReq)
                .retrieve()
                .toEntity(WebDtos.PayResponse.class);
        assertThat(payResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(payResp.getBody().settled()).isTrue();
        assertThat(payResp.getBody().endToEndId()).startsWith("E");
    }
}

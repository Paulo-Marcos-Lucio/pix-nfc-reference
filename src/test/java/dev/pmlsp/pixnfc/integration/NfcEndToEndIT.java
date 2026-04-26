package dev.pmlsp.pixnfc.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pmlsp.pixnfc.adapter.web.dto.WebDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o fluxo completo end-to-end via HTTP — issue → validate → pay —
 * batendo no simulator embutido (DICT + SPI).
 */
class NfcEndToEndIT extends AbstractIntegrationIT {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;

    @Test
    void fullFlowIssueValidateAndPay() {
        WebDtos.IssueChargeRequest issueReq = new WebDtos.IssueChargeRequest(
                "T-IT-1", 250L, "Padaria · IT", 120);
        ResponseEntity<WebDtos.IssueChargeResponse> issueResp = http.postForEntity(
                URI.create("http://localhost:" + port() + "/v1/nfc/charges"),
                jsonEntity(issueReq),
                WebDtos.IssueChargeResponse.class);
        assertThat(issueResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(issueResp.getBody()).isNotNull();
        String wire = issueResp.getBody().payloadWire();
        assertThat(wire).isNotBlank();

        WebDtos.ValidatePayloadRequest validateReq = new WebDtos.ValidatePayloadRequest(wire);
        ResponseEntity<WebDtos.ValidatePayloadResponse> validateResp = http.postForEntity(
                URI.create("http://localhost:" + port() + "/v1/nfc/payments/validate"),
                jsonEntity(validateReq),
                WebDtos.ValidatePayloadResponse.class);
        assertThat(validateResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(validateResp.getBody().amountCents()).isEqualTo(250L);

        WebDtos.PayRequest payReq = new WebDtos.PayRequest(
                wire,
                new WebDtos.AccountDto("99988877", "0001", "1234567", "CACC"),
                "device-it-1");
        ResponseEntity<WebDtos.PayResponse> payResp = http.postForEntity(
                URI.create("http://localhost:" + port() + "/v1/nfc/payments"),
                jsonEntity(payReq),
                WebDtos.PayResponse.class);
        assertThat(payResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(payResp.getBody().settled()).isTrue();
        assertThat(payResp.getBody().endToEndId()).startsWith("E");
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}

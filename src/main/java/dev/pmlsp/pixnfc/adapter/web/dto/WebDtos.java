package dev.pmlsp.pixnfc.adapter.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs do API HTTP. Mantidos desacoplados do domain pra que mudanças no
 * contrato externo não vazem para use cases.
 */
public final class WebDtos {

    private WebDtos() {}

    // ----- Issue charge (lado recebedor) -----

    public record IssueChargeRequest(
            @NotBlank String terminalId,
            @Min(1) long amountCents,
            String displayLabel,
            @Min(10) int validitySeconds) {}

    public record IssueChargeResponse(
            UUID chargeId,
            String terminalId,
            long amountCents,
            String displayLabel,
            String status,
            Instant issuedAt,
            Instant expiresAt,
            String payloadWire,
            String merchantKeyMasked) {}

    // ----- Validate payload (lado pagador) -----

    public record ValidatePayloadRequest(@NotBlank String payloadWire) {}

    public record ValidatePayloadResponse(
            UUID chargeId,
            String merchantIspb,
            String merchantKeyMasked,
            long amountCents,
            String displayLabel,
            Instant issuedAt,
            Instant expiresAt) {}

    // ----- Pay (lado pagador) -----

    public record PayRequest(
            @NotBlank String payloadWire,
            @NotNull AccountDto payerAccount,
            String payerDeviceId) {}

    public record PayResponse(
            UUID chargeId,
            boolean settled,
            String endToEndId,
            Instant settledAt,
            String failureReason) {}

    // ----- Common -----

    public record AccountDto(
            @NotBlank String ispb,
            @NotBlank String branch,
            @NotBlank String number,
            @NotBlank String type) {}
}

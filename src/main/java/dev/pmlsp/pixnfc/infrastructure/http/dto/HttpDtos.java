package dev.pmlsp.pixnfc.infrastructure.http.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire-format DTOs trocados com DICT (lookup) e SPI (settle). Mantidos
 * desacoplados do domain para que mudanças de naming (XML/JSON BCB) não
 * vazem para use cases.
 */
public final class HttpDtos {

    private HttpDtos() {}

    // ----- DICT -----

    public record KeyPayload(String type, String value) {}

    public record AccountPayload(String ispb, String branch, String number, String type) {}

    public record OwnerPayload(String type, String document, String name, String tradeName) {}

    public record EntryPayload(
            KeyPayload key,
            AccountPayload account,
            OwnerPayload owner,
            Instant createdAt,
            Instant keyOwnershipDate) {}

    public record ProblemPayload(String code, String message) {}

    // ----- SPI -----

    public record SpiSettleRequest(
            UUID chargeId,
            String merchantIspb,
            KeyPayload merchantKey,
            long amountCents,
            AccountPayload payerAccount,
            String terminalId) {}

    public record SpiSettleResponse(
            boolean settled,
            String endToEndId,
            Instant settledAt,
            String failureReason) {}
}

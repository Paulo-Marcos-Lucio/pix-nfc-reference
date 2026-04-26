package dev.pmlsp.pixnfc.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

/**
 * Estado server-side de uma cobrança NFC emitida pela maquininha.
 *
 * <p>Representa o ciclo de vida no PSP recebedor: criada quando a maquininha
 * solicita o payload, transita para CONFIRMED quando o SPI notifica liquidação,
 * ou EXPIRED se a janela de validade vence sem pagamento.
 */
@Value
@Builder
@With
public class NfcCharge {

    UUID chargeId;
    String terminalId;
    Ispb merchantIspb;
    PixKey merchantKey;
    long amountCents;
    String displayLabel;
    Instant issuedAt;
    int validitySeconds;
    ChargeStatus status;
    String endToEndId;
    Instant settledAt;
    Reason failureReason;

    public NfcCharge confirm(String endToEndId, Instant settledAt) {
        if (status != ChargeStatus.PENDING) {
            throw new IllegalStateException("cannot confirm charge in status " + status);
        }
        return this.withStatus(ChargeStatus.CONFIRMED)
                .withEndToEndId(endToEndId)
                .withSettledAt(settledAt);
    }

    public NfcCharge expire() {
        if (status != ChargeStatus.PENDING) return this;
        return this.withStatus(ChargeStatus.EXPIRED);
    }

    public NfcCharge fail(Reason reason) {
        return this.withStatus(ChargeStatus.FAILED).withFailureReason(reason);
    }
}

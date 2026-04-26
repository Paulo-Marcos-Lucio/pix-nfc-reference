package dev.pmlsp.pixnfc.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Resultado de uma operação Pix originada de NFC. Carrega o endToEndId
 * canônico ISO 20022 quando bem-sucedida, ou um motivo de falha estruturado.
 */
@Value
@Builder
public class PixOperationResult {

    /** {@code true} se SPI confirmou liquidação. */
    boolean settled;

    /**
     * Identificador único ISO 20022 da transação Pix end-to-end.
     * Formato: E + 8 dígitos ISPB pagador + 16 dígitos timestamp + 8 alfanuméricos.
     * Presente apenas em settled == true.
     */
    String endToEndId;

    /** Quando o SPI confirmou. */
    Instant settledAt;

    /** Razão estruturada da falha. Presente apenas em settled == false. */
    Reason failureReason;
}

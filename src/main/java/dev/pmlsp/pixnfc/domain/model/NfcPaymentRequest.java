package dev.pmlsp.pixnfc.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Solicitação de execução de pagamento Pix originada da leitura NFC pelo
 * app do pagador. O backend do PSP pagador valida o payload, autoriza no
 * cliente, e dispara o Pix via SPI.
 */
@Value
@Builder
public class NfcPaymentRequest {

    /** Payload NFC original lido via HCE. */
    NfcPayload payload;

    /** Conta do pagador a ser debitada. */
    Account payerAccount;

    /** ID do dispositivo que leu o payload (controle antifraude). */
    String payerDeviceId;
}

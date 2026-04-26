package dev.pmlsp.pixnfc.domain.port.in;

import dev.pmlsp.pixnfc.domain.model.NfcPaymentRequest;
import dev.pmlsp.pixnfc.domain.model.PixOperationResult;

/**
 * Lado do PSP pagador: app leu payload NFC via HCE e pede execução.
 * Backend valida, autoriza, dispara Pix via SPI.
 */
public interface ProcessPaymentUseCase {

    PixOperationResult process(NfcPaymentRequest request);
}

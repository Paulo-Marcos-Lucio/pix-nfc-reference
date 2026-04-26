package dev.pmlsp.pixnfc.domain.port.in;

import dev.pmlsp.pixnfc.domain.model.NfcPayload;

/**
 * Validação isolada de payload NFC sem disparar Pix.
 * Útil pro app exibir confirmação ao pagador antes de autorizar.
 */
public interface ValidatePayloadUseCase {

    /**
     * @throws dev.pmlsp.pixnfc.domain.exception.InvalidPayloadException
     *         quando assinatura/expiração/integridade falham
     */
    void validate(NfcPayload payload);
}

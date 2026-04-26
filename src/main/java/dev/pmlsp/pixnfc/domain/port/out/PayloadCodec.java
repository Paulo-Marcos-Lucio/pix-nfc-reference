package dev.pmlsp.pixnfc.domain.port.out;

import dev.pmlsp.pixnfc.domain.model.NfcPayload;

/**
 * Codec de payload NFC: serializa/desserializa o formato wire e calcula/valida
 * assinatura HMAC. Implementação concreta vive em infrastructure.
 */
public interface PayloadCodec {

    /** Calcula a assinatura HMAC e retorna o payload com {@code signature} preenchido. */
    NfcPayload sign(NfcPayload unsigned);

    /** Verifica a assinatura. Lança {@code InvalidPayloadException} se inválida. */
    void verify(NfcPayload payload);

    /** Codifica em bytes pra transmissão via NFC (formato BR Code adaptado). */
    byte[] encode(NfcPayload payload);

    /** Decodifica bytes recebidos pela leitura HCE em {@link NfcPayload}. */
    NfcPayload decode(byte[] wire);
}

package dev.pmlsp.pixnfc.domain.exception;

/**
 * Payload NFC rejeitado por motivo estrutural ou criptográfico —
 * assinatura inválida, expirado, malformado, ou inconsistente com a chave Pix
 * resolvida no DICT.
 */
public class InvalidPayloadException extends DictException {

    private final String reasonCode;

    public InvalidPayloadException(String reasonCode, String message) {
        super("invalid NFC payload [%s]: %s".formatted(reasonCode, message));
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}

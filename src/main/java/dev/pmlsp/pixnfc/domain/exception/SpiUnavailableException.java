package dev.pmlsp.pixnfc.domain.exception;

/**
 * SPI (Sistema de Pagamentos Instantâneos do BCB) ficou indisponível
 * ou demorou além do SLA configurado. Caller deve fazer fallback gracioso —
 * estado da transação fica {@code PENDING} e job de reconciliação assíncrona
 * resolve quando SPI voltar.
 */
public class SpiUnavailableException extends DictException {

    public SpiUnavailableException(String message) {
        super(message);
    }

    public SpiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

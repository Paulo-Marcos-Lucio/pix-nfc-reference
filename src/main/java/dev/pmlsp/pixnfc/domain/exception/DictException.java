package dev.pmlsp.pixnfc.domain.exception;

/**
 * Tipo base para qualquer erro originado nas integrações deste cliente
 * (DICT do BCB, SPI do BCB, validação de payload NFC). Subtipos cobrem
 * falhas taxonomizadas que cada API regulatória pode expor.
 */
public abstract class DictException extends RuntimeException {

    protected DictException(String message) {
        super(message);
    }

    protected DictException(String message, Throwable cause) {
        super(message, cause);
    }
}

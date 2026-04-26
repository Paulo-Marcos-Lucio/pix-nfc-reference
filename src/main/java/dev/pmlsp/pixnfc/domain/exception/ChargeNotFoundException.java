package dev.pmlsp.pixnfc.domain.exception;

import java.util.UUID;

/**
 * Cobrança NFC referida por {@code chargeId} não existe no PSP recebedor.
 * Indica payload forjado, repetido após expiração, ou bug de roteamento.
 */
public class ChargeNotFoundException extends DictException {

    public ChargeNotFoundException(UUID chargeId) {
        super("charge not found: " + chargeId);
    }
}

package dev.pmlsp.pixnfc.domain.port.out;

import dev.pmlsp.pixnfc.domain.model.Account;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;
import dev.pmlsp.pixnfc.domain.model.PixOperationResult;

/**
 * Porta de saída para o SPI (Sistema de Pagamentos Instantâneos do BCB) —
 * canal real de liquidação Pix. Implementação concreta usa mensageria ISO 20022
 * (pacs.008) com mTLS ICP-Brasil.
 */
public interface PixSpiGateway {

    PixOperationResult settle(NfcPayload payload, Account payerAccount);
}

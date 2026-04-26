package dev.pmlsp.pixnfc.domain.port.in;

import dev.pmlsp.pixnfc.domain.model.NfcCharge;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;

/**
 * Lado do PSP recebedor: a maquininha pede um payload NFC pra emitir.
 * Retorna o {@link NfcCharge} persistido + o {@link NfcPayload} assinado
 * pronto pra ser transmitido via NFC.
 */
public interface IssueChargeUseCase {

    record IssueChargeCommand(
            String terminalId,
            long amountCents,
            String displayLabel,
            int validitySeconds
    ) {}

    record IssueChargeResult(NfcCharge charge, NfcPayload payload) {}

    IssueChargeResult issue(IssueChargeCommand command);
}

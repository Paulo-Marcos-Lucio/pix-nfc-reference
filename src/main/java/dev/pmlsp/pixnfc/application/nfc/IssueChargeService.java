package dev.pmlsp.pixnfc.application.nfc;

import dev.pmlsp.pixnfc.domain.model.ChargeStatus;
import dev.pmlsp.pixnfc.domain.model.MerchantContext;
import dev.pmlsp.pixnfc.domain.model.NfcCharge;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;
import dev.pmlsp.pixnfc.domain.port.in.IssueChargeUseCase;
import dev.pmlsp.pixnfc.domain.port.out.ChargeRepository;
import dev.pmlsp.pixnfc.domain.port.out.PayloadCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Lado do PSP recebedor: gera payload NFC assinado e persiste a cobrança
 * no estado {@code PENDING}. A maquininha então emite o payload via NFC
 * pra leitura HCE pelo dispositivo do pagador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueChargeService implements IssueChargeUseCase {

    private final ChargeRepository repository;
    private final PayloadCodec codec;
    private final MerchantContext merchant;
    private final Clock clock;

    @Override
    public IssueChargeResult issue(IssueChargeCommand command) {
        Instant now = clock.instant();
        UUID chargeId = UUID.randomUUID();

        NfcPayload unsigned = NfcPayload.builder()
                .chargeId(chargeId)
                .merchantIspb(merchant.ispb())
                .merchantKey(merchant.pixKey())
                .amountCents(command.amountCents())
                .terminalId(command.terminalId())
                .displayLabel(command.displayLabel())
                .issuedAt(now)
                .validitySeconds(command.validitySeconds())
                .build();
        NfcPayload signed = codec.sign(unsigned);

        NfcCharge charge = NfcCharge.builder()
                .chargeId(chargeId)
                .terminalId(command.terminalId())
                .merchantIspb(merchant.ispb())
                .merchantKey(merchant.pixKey())
                .amountCents(command.amountCents())
                .displayLabel(command.displayLabel())
                .issuedAt(now)
                .validitySeconds(command.validitySeconds())
                .status(ChargeStatus.PENDING)
                .build();
        NfcCharge saved = repository.save(charge);

        log.info("nfc charge issued chargeId={} terminal={} amountCents={}",
                chargeId, command.terminalId(), command.amountCents());

        return new IssueChargeResult(saved, signed);
    }
}

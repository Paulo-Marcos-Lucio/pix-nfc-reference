package dev.pmlsp.pixnfc.application.nfc;

import dev.pmlsp.pixnfc.domain.model.NfcPaymentRequest;
import dev.pmlsp.pixnfc.domain.model.PixOperationResult;
import dev.pmlsp.pixnfc.domain.port.in.ProcessPaymentUseCase;
import dev.pmlsp.pixnfc.domain.port.in.ValidatePayloadUseCase;
import dev.pmlsp.pixnfc.domain.port.out.PixSpiGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lado do PSP pagador: orquestra validate + settle.
 * <ol>
 *   <li>{@link ValidatePayloadUseCase} confere assinatura, expiração, e DICT cross-check</li>
 *   <li>{@link PixSpiGateway} dispara o Pix via SPI usando os dados do payload</li>
 * </ol>
 *
 * Idempotência é responsabilidade do {@code Idempotency-Key} no controller —
 * neste service, qualquer reentrada com mesmo payload+payer é tratada como
 * nova tentativa (use cache externo se quiser deduplicação dentro do TTL).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final ValidatePayloadUseCase validator;
    private final PixSpiGateway spi;

    @Override
    public PixOperationResult process(NfcPaymentRequest request) {
        validator.validate(request.getPayload());

        PixOperationResult result = spi.settle(request.getPayload(), request.getPayerAccount());

        log.info("nfc payment processed chargeId={} settled={} endToEndId={}",
                request.getPayload().getChargeId(), result.isSettled(), result.getEndToEndId());

        return result;
    }
}

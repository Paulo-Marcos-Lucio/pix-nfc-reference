package dev.pmlsp.pixnfc.adapter.web;

import dev.pmlsp.pixnfc.adapter.web.dto.WebDtos;
import dev.pmlsp.pixnfc.domain.model.Account;
import dev.pmlsp.pixnfc.domain.model.AccountType;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.NfcCharge;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;
import dev.pmlsp.pixnfc.domain.model.NfcPaymentRequest;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.model.PixOperationResult;
import dev.pmlsp.pixnfc.domain.port.in.IssueChargeUseCase;
import dev.pmlsp.pixnfc.domain.port.in.IssueChargeUseCase.IssueChargeCommand;
import dev.pmlsp.pixnfc.domain.port.in.ProcessPaymentUseCase;
import dev.pmlsp.pixnfc.domain.port.in.ValidatePayloadUseCase;
import dev.pmlsp.pixnfc.domain.port.out.PayloadCodec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Base64;

/**
 * Demo HTTP facade sobre os use cases — útil pra sessões de {@code requests.http}
 * e testes de integração. Adopters reais geralmente injetam as use case interfaces
 * diretamente em vez de chamar este controller.
 *
 * <p>Endpoints:
 * <ul>
 *   <li><strong>POST /v1/nfc/charges</strong> — lado recebedor: gera payload assinado pra emitir via NFC</li>
 *   <li><strong>POST /v1/nfc/payments/validate</strong> — lado pagador: valida payload sem cobrar</li>
 *   <li><strong>POST /v1/nfc/payments</strong> — lado pagador: valida + dispara Pix via SPI</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/nfc")
@RequiredArgsConstructor
public class PixNfcFacadeController {

    private final IssueChargeUseCase issue;
    private final ValidatePayloadUseCase validate;
    private final ProcessPaymentUseCase process;
    private final PayloadCodec codec;

    @PostMapping("/charges")
    public ResponseEntity<WebDtos.IssueChargeResponse> issueCharge(@Valid @RequestBody WebDtos.IssueChargeRequest body) {
        IssueChargeUseCase.IssueChargeResult result = issue.issue(new IssueChargeCommand(
                body.terminalId(),
                body.amountCents(),
                body.displayLabel(),
                body.validitySeconds()));
        byte[] wire = codec.encode(result.payload());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(result.charge(), result.payload(), wire));
    }

    @PostMapping("/payments/validate")
    public ResponseEntity<WebDtos.ValidatePayloadResponse> validatePayload(@Valid @RequestBody WebDtos.ValidatePayloadRequest body) {
        NfcPayload payload = decode(body.payloadWire());
        validate.validate(payload);
        return ResponseEntity.ok(new WebDtos.ValidatePayloadResponse(
                payload.getChargeId(),
                payload.getMerchantIspb().value(),
                payload.getMerchantKey().masked(),
                payload.getAmountCents(),
                payload.getDisplayLabel(),
                payload.getIssuedAt(),
                payload.getIssuedAt().plusSeconds(payload.getValiditySeconds())));
    }

    @PostMapping("/payments")
    public ResponseEntity<WebDtos.PayResponse> processPayment(@Valid @RequestBody WebDtos.PayRequest body) {
        NfcPayload payload = decode(body.payloadWire());
        Account payerAccount = new Account(
                Ispb.of(body.payerAccount().ispb()),
                body.payerAccount().branch(),
                body.payerAccount().number(),
                AccountType.valueOf(body.payerAccount().type()));
        PixOperationResult result = process.process(NfcPaymentRequest.builder()
                .payload(payload)
                .payerAccount(payerAccount)
                .payerDeviceId(body.payerDeviceId())
                .build());
        return ResponseEntity.ok(new WebDtos.PayResponse(
                payload.getChargeId(),
                result.isSettled(),
                result.getEndToEndId(),
                result.getSettledAt(),
                result.getFailureReason() == null ? null : result.getFailureReason().name()));
    }

    private NfcPayload decode(String wireBase64) {
        return codec.decode(wireBase64.getBytes());
    }

    private static WebDtos.IssueChargeResponse toResponse(NfcCharge charge, NfcPayload payload, byte[] wire) {
        return new WebDtos.IssueChargeResponse(
                charge.getChargeId(),
                charge.getTerminalId(),
                charge.getAmountCents(),
                charge.getDisplayLabel(),
                charge.getStatus().name(),
                charge.getIssuedAt(),
                Instant.from(charge.getIssuedAt()).plusSeconds(charge.getValiditySeconds()),
                new String(wire),
                payload.getMerchantKey().masked());
    }

    private static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}

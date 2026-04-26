package dev.pmlsp.pixnfc.application.nfc;

import dev.pmlsp.pixnfc.domain.exception.InvalidPayloadException;
import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;
import dev.pmlsp.pixnfc.domain.port.in.LookupKeyUseCase;
import dev.pmlsp.pixnfc.domain.port.in.LookupKeyUseCase.LookupQuery;
import dev.pmlsp.pixnfc.domain.port.in.ValidatePayloadUseCase;
import dev.pmlsp.pixnfc.domain.port.out.PayloadCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Validação completa de payload NFC: assinatura HMAC, expiração, e
 * cross-check com DICT (a chave declarada pertence mesmo ao ISPB declarado).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidatePayloadService implements ValidatePayloadUseCase {

    private final PayloadCodec codec;
    private final LookupKeyUseCase lookup;
    private final Clock clock;

    @Override
    public void validate(NfcPayload payload) {
        codec.verify(payload);

        if (payload.isExpired(clock.instant())) {
            throw new InvalidPayloadException("EXPIRED",
                    "payload expired at " + payload.getIssuedAt().plusSeconds(payload.getValiditySeconds()));
        }

        DictEntry entry = lookup.lookup(new LookupQuery(
                payload.getMerchantKey(),
                payload.getMerchantIspb()));

        if (!entry.getAccount().ispb().equals(payload.getMerchantIspb())) {
            throw new InvalidPayloadException("MERCHANT_ISPB_MISMATCH",
                    "DICT entry ISPB does not match payload");
        }
    }
}

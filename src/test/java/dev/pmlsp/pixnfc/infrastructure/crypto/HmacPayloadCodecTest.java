package dev.pmlsp.pixnfc.infrastructure.crypto;

import dev.pmlsp.pixnfc.domain.exception.InvalidPayloadException;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.model.PixKeyType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacPayloadCodecTest {

    private static final String KEY = "test-hmac-key-with-enough-entropy-12345";
    private final HmacPayloadCodec codec = new HmacPayloadCodec(KEY, "HmacSHA256", Clock.systemUTC());

    private NfcPayload sample() {
        return NfcPayload.builder()
                .chargeId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .merchantIspb(Ispb.of("12345678"))
                .merchantKey(PixKey.of(PixKeyType.CNPJ, "12345678000199"))
                .amountCents(1500L)
                .terminalId("T-001")
                .displayLabel("Teste · #1")
                .issuedAt(Instant.parse("2026-04-26T12:00:00Z"))
                .validitySeconds(120)
                .build();
    }

    @Test
    void signProducesDeterministicSignature() {
        NfcPayload signed1 = codec.sign(sample());
        NfcPayload signed2 = codec.sign(sample());
        assertThat(signed1.getSignature())
                .isNotBlank()
                .isEqualTo(signed2.getSignature());
    }

    @Test
    void verifyAcceptsValidSignature() {
        NfcPayload signed = codec.sign(sample());
        codec.verify(signed); // does not throw
    }

    @Test
    void verifyRejectsTamperedAmount() {
        NfcPayload signed = codec.sign(sample());
        NfcPayload tampered = NfcPayload.builder()
                .chargeId(signed.getChargeId())
                .merchantIspb(signed.getMerchantIspb())
                .merchantKey(signed.getMerchantKey())
                .amountCents(9999L)
                .terminalId(signed.getTerminalId())
                .displayLabel(signed.getDisplayLabel())
                .issuedAt(signed.getIssuedAt())
                .validitySeconds(signed.getValiditySeconds())
                .signature(signed.getSignature())
                .build();
        assertThatThrownBy(() -> codec.verify(tampered))
                .isInstanceOf(InvalidPayloadException.class)
                .hasMessageContaining("INVALID_SIGNATURE");
    }

    @Test
    void verifyRejectsMissingSignature() {
        assertThatThrownBy(() -> codec.verify(sample()))
                .isInstanceOf(InvalidPayloadException.class)
                .hasMessageContaining("MISSING_SIGNATURE");
    }

    @Test
    void encodeDecodeRoundTripsAllFields() {
        NfcPayload signed = codec.sign(sample());
        byte[] wire = codec.encode(signed);
        NfcPayload decoded = codec.decode(wire);
        assertThat(decoded.getChargeId()).isEqualTo(signed.getChargeId());
        assertThat(decoded.getMerchantIspb()).isEqualTo(signed.getMerchantIspb());
        assertThat(decoded.getMerchantKey()).isEqualTo(signed.getMerchantKey());
        assertThat(decoded.getAmountCents()).isEqualTo(signed.getAmountCents());
        assertThat(decoded.getTerminalId()).isEqualTo(signed.getTerminalId());
        assertThat(decoded.getDisplayLabel()).isEqualTo(signed.getDisplayLabel());
        assertThat(decoded.getIssuedAt()).isEqualTo(signed.getIssuedAt());
        assertThat(decoded.getValiditySeconds()).isEqualTo(signed.getValiditySeconds());
        assertThat(decoded.getSignature()).isEqualTo(signed.getSignature());
        codec.verify(decoded);
    }
}

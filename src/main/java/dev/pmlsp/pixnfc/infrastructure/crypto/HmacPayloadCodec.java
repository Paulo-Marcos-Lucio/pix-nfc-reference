package dev.pmlsp.pixnfc.infrastructure.crypto;

import dev.pmlsp.pixnfc.domain.exception.InvalidPayloadException;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.NfcPayload;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.model.PixKeyType;
import dev.pmlsp.pixnfc.domain.port.out.PayloadCodec;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Codec HMAC-SHA256 para payloads NFC.
 *
 * <p>Wire format (field-separator {@code "|"}, codificação UTF-8):
 * <pre>
 * BR1|{chargeId}|{merchantIspb}|{keyType}:{keyValue}|{amountCents}|{terminalId}|{label}|{issuedAt}|{validitySec}|{signature}
 * </pre>
 *
 * <p>A assinatura é o HMAC do bloco antes do último separador. {@link MessageDigest#isEqual}
 * é usado pra comparação constant-time, evitando timing attacks.
 *
 * <p>Em produção real recomenda-se ECDSA com cert ICP-Brasil em vez de HMAC simétrico —
 * o reference usa HMAC pra simplicidade de demonstração.
 */
@Slf4j
public class HmacPayloadCodec implements PayloadCodec {

    private static final String VERSION = "BR1";
    private static final String SEP = "|";

    private final byte[] keyBytes;
    private final String algorithm;
    private final Clock clock;

    public HmacPayloadCodec(String hmacKey, String algorithm, Clock clock) {
        this.keyBytes = hmacKey.getBytes(StandardCharsets.UTF_8);
        this.algorithm = algorithm == null ? "HmacSHA256" : algorithm;
        this.clock = clock;
    }

    @Override
    public NfcPayload sign(NfcPayload unsigned) {
        String canonical = canonical(unsigned);
        String sig = hmacHex(canonical);
        return NfcPayload.builder()
                .chargeId(unsigned.getChargeId())
                .merchantIspb(unsigned.getMerchantIspb())
                .merchantKey(unsigned.getMerchantKey())
                .amountCents(unsigned.getAmountCents())
                .terminalId(unsigned.getTerminalId())
                .displayLabel(unsigned.getDisplayLabel())
                .issuedAt(unsigned.getIssuedAt())
                .validitySeconds(unsigned.getValiditySeconds())
                .signature(sig)
                .build();
    }

    @Override
    public void verify(NfcPayload payload) {
        if (payload.getSignature() == null || payload.getSignature().isBlank()) {
            throw new InvalidPayloadException("MISSING_SIGNATURE", "payload has no signature");
        }
        String expected = hmacHex(canonical(payload));
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = payload.getSignature().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            throw new InvalidPayloadException("INVALID_SIGNATURE",
                    "HMAC verification failed for charge " + payload.getChargeId());
        }
    }

    @Override
    public byte[] encode(NfcPayload payload) {
        if (payload.getSignature() == null) {
            throw new IllegalStateException("payload must be signed before encode");
        }
        String wire = canonical(payload) + SEP + payload.getSignature();
        return Base64.getEncoder().encode(wire.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public NfcPayload decode(byte[] wire) {
        try {
            String text = new String(Base64.getDecoder().decode(wire), StandardCharsets.UTF_8);
            String[] parts = text.split("\\|");
            if (parts.length != 10 || !VERSION.equals(parts[0])) {
                throw new InvalidPayloadException("MALFORMED", "expected 10 fields prefixed by " + VERSION);
            }
            String[] keyParts = parts[3].split(":", 2);
            if (keyParts.length != 2) {
                throw new InvalidPayloadException("MALFORMED_KEY", "expected keyType:keyValue");
            }
            return NfcPayload.builder()
                    .chargeId(UUID.fromString(parts[1]))
                    .merchantIspb(Ispb.of(parts[2]))
                    .merchantKey(PixKey.of(PixKeyType.valueOf(keyParts[0]), keyParts[1]))
                    .amountCents(Long.parseLong(parts[4]))
                    .terminalId(parts[5])
                    .displayLabel(parts[6])
                    .issuedAt(Instant.parse(parts[7]))
                    .validitySeconds(Integer.parseInt(parts[8]))
                    .signature(parts[9])
                    .build();
        } catch (InvalidPayloadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPayloadException("DECODE_ERROR", "failed to decode wire: " + e.getMessage());
        }
    }

    private String canonical(NfcPayload p) {
        return String.join(SEP,
                VERSION,
                String.valueOf(p.getChargeId()),
                p.getMerchantIspb().value(),
                p.getMerchantKey().type().name() + ":" + p.getMerchantKey().value(),
                String.valueOf(p.getAmountCents()),
                emptyIfNull(p.getTerminalId()),
                emptyIfNull(p.getDisplayLabel()),
                p.getIssuedAt().toString(),
                String.valueOf(p.getValiditySeconds()));
    }

    private String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(keyBytes, algorithm));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("unsupported HMAC algorithm: " + algorithm, e);
        } catch (java.security.InvalidKeyException e) {
            throw new IllegalStateException("invalid HMAC key", e);
        }
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }
}

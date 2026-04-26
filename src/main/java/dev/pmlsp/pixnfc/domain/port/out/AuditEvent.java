package dev.pmlsp.pixnfc.domain.port.out;

import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.model.PixKeyType;

import java.time.Duration;
import java.time.Instant;

/**
 * Compliance-grade record of one DICT operation. Field {@code keyMasked} is the
 * obfuscated representation produced by {@link PixKey#masked()} — never the raw key.
 */
public record AuditEvent(
        Instant timestamp,
        String operation,
        Ispb requesterIspb,
        String keyMasked,
        PixKeyType keyType,
        Outcome outcome,
        String errorCode,
        Duration duration) {

    public enum Outcome { SUCCESS, CACHE_HIT, NOT_FOUND, ERROR }

    public static AuditEvent success(String operation, Ispb requesterIspb, PixKey key, Duration duration) {
        return new AuditEvent(Instant.now(), operation, requesterIspb,
                key == null ? null : key.masked(),
                key == null ? null : key.type(),
                Outcome.SUCCESS, null, duration);
    }

    public static AuditEvent cacheHit(String operation, Ispb requesterIspb, PixKey key, Duration duration) {
        return new AuditEvent(Instant.now(), operation, requesterIspb,
                key.masked(), key.type(),
                Outcome.CACHE_HIT, null, duration);
    }

    public static AuditEvent notFound(String operation, Ispb requesterIspb, PixKey key, Duration duration) {
        return new AuditEvent(Instant.now(), operation, requesterIspb,
                key.masked(), key.type(),
                Outcome.NOT_FOUND, null, duration);
    }

    public static AuditEvent error(String operation, Ispb requesterIspb, PixKey key, String errorCode, Duration duration) {
        return new AuditEvent(Instant.now(), operation, requesterIspb,
                key == null ? null : key.masked(),
                key == null ? null : key.type(),
                Outcome.ERROR, errorCode, duration);
    }
}

package dev.pmlsp.pixnfc.infrastructure.audit;

import dev.pmlsp.pixnfc.domain.port.out.AuditEvent;
import dev.pmlsp.pixnfc.domain.port.out.AuditLog;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;

/**
 * Single sink for both structured audit logging (JSON via Logstash encoder) and
 * Micrometer metrics derived from the same event. Centralizing the two avoids
 * duplicating instrumentation across each use-case service.
 *
 * <p>Metrics emitted:
 * <ul>
 *   <li>{@code dict.operation.duration} — Timer (histogram), tags: {@code op, outcome}</li>
 *   <li>{@code dict.cache.hit} — Counter, tag {@code keyType}</li>
 *   <li>{@code dict.cache.miss} — Counter, tag {@code keyType} — derived from successful lookups
 *       that didn't come from cache (i.e. operation=lookup, outcome=success)</li>
 *   <li>{@code dict.gateway.errors} — Counter, tag {@code errorClass}</li>
 * </ul>
 *
 * <p>All log fields are prefixed with {@code dict.} so they're easy to filter in Loki
 * or any JSON log backend.
 */
@Slf4j(topic = "dict.audit")
@Component
@RequiredArgsConstructor
public class StructuredAuditLog implements AuditLog {

    private static final String OP_LOOKUP = "lookup";

    private final MeterRegistry registry;

    @Override
    public void record(AuditEvent event) {
        log.info("dict.op",
                StructuredArguments.kv("dict.op", event.operation()),
                StructuredArguments.kv("dict.requesterIspb",
                        event.requesterIspb() == null ? null : event.requesterIspb().value()),
                StructuredArguments.kv("dict.keyType", event.keyType()),
                StructuredArguments.kv("dict.keyMasked", event.keyMasked()),
                StructuredArguments.kv("dict.outcome", event.outcome().name()),
                StructuredArguments.kv("dict.errorCode", event.errorCode()),
                StructuredArguments.kv("dict.durationMs", event.duration().toMillis()));

        Timer.builder("dict.operation.duration")
                .tag("op", event.operation())
                .tag("outcome", event.outcome().name().toLowerCase())
                .register(registry)
                .record(event.duration());

        switch (event.outcome()) {
            case CACHE_HIT -> {
                if (event.keyType() != null) {
                    registry.counter("dict.cache.hit", "keyType", event.keyType().name()).increment();
                }
            }
            case SUCCESS -> {
                // a lookup that reaches gateway is, by definition, a cache miss
                if (OP_LOOKUP.equals(event.operation()) && event.keyType() != null) {
                    registry.counter("dict.cache.miss", "keyType", event.keyType().name()).increment();
                }
            }
            case NOT_FOUND -> {
                if (OP_LOOKUP.equals(event.operation()) && event.keyType() != null) {
                    registry.counter("dict.cache.miss", "keyType", event.keyType().name()).increment();
                }
            }
            default -> { /* no extra counter */ }
        }

        if (event.errorCode() != null) {
            registry.counter("dict.gateway.errors", "errorClass", event.errorCode()).increment();
        }
    }
}

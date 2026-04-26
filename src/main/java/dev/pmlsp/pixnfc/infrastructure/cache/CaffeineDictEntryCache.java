package dev.pmlsp.pixnfc.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.port.out.DictEntryCache;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.Optional;

/**
 * Caffeine-backed cache that supports a different TTL per entry — required because
 * each {@link PixKey} type has its own regulatory cap and entries with open claims
 * must not be cached at all. Registers a {@code dict.cache.size} gauge so dashboards
 * can show estimated entry count over time.
 */
public class CaffeineDictEntryCache implements DictEntryCache {

    private final Cache<PixKey, Entry> cache;

    public CaffeineDictEntryCache(int maxSize, MeterRegistry registry) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfter(new Expiry<PixKey, Entry>() {
                    @Override
                    public long expireAfterCreate(PixKey key, Entry value, long currentTime) {
                        return value.ttl().toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(PixKey key, Entry value, long currentTime, long currentDuration) {
                        return value.ttl().toNanos();
                    }

                    @Override
                    public long expireAfterRead(PixKey key, Entry value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();

        if (registry != null) {
            Gauge.builder("dict.cache.size", cache, c -> (double) c.estimatedSize())
                    .description("Estimated number of entries currently in the DICT lookup cache")
                    .baseUnit("entries")
                    .register(registry);
            Gauge.builder("dict.cache.max_size", cache, c -> (double) maxSize)
                    .description("Configured maximum size of the DICT lookup cache")
                    .baseUnit("entries")
                    .register(registry);
        }
    }

    @Override
    public Optional<DictEntry> get(PixKey key) {
        Entry stored = cache.getIfPresent(key);
        return stored == null ? Optional.empty() : Optional.of(stored.value());
    }

    @Override
    public void put(PixKey key, DictEntry entry, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        cache.put(key, new Entry(entry, ttl));
    }

    @Override
    public void invalidate(PixKey key) {
        cache.invalidate(key);
    }

    private record Entry(DictEntry value, Duration ttl) {}
}

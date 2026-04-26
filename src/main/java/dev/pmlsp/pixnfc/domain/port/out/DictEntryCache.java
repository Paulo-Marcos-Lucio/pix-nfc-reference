package dev.pmlsp.pixnfc.domain.port.out;

import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.PixKey;

import java.time.Duration;
import java.util.Optional;

public interface DictEntryCache {

    Optional<DictEntry> get(PixKey key);

    /**
     * Store an entry with the given TTL. Implementations MUST honor the TTL and
     * MUST treat a zero/negative TTL as "do not cache".
     */
    void put(PixKey key, DictEntry entry, Duration ttl);

    void invalidate(PixKey key);
}

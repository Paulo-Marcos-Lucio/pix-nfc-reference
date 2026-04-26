package dev.pmlsp.pixnfc.domain.policy;

import dev.pmlsp.pixnfc.domain.model.DictEntry;

import java.time.Duration;

/**
 * Pure-domain policy that decides how long a {@link DictEntry} may live in the local cache,
 * given the key type and the entry's own claim state.
 *
 * <p>Implementations MUST clamp any caller-provided TTL to the regulatory maximum exposed
 * via the constants below. Entries with open claims MUST always return {@link #ZERO}.
 *
 * <p>The maxima below are sourced from the BCB DICT operations manual; check the manual
 * version in your environment as part of compliance review.
 */
public interface CacheTtlPolicy {

    Duration ZERO = Duration.ZERO;

    /** Maximum TTL allowed by BCB for CPF keys. */
    Duration MAX_CPF = Duration.ofSeconds(60);
    /** Maximum TTL allowed by BCB for CNPJ keys. */
    Duration MAX_CNPJ = Duration.ofSeconds(300);
    /** Maximum TTL allowed by BCB for EMAIL keys. */
    Duration MAX_EMAIL = Duration.ofSeconds(300);
    /** Maximum TTL allowed by BCB for PHONE keys. */
    Duration MAX_PHONE = Duration.ofSeconds(300);
    /** Maximum TTL allowed by BCB for EVP (random) keys. */
    Duration MAX_EVP = Duration.ofSeconds(60);

    /**
     * @return the TTL that the caller MAY use to cache this entry locally.
     *         Returns {@link #ZERO} when caching is forbidden (e.g. claim in progress).
     */
    Duration ttlFor(DictEntry entry);
}

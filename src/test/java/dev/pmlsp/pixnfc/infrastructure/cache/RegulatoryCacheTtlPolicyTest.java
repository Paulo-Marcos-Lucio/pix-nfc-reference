package dev.pmlsp.pixnfc.infrastructure.cache;

import dev.pmlsp.pixnfc.domain.model.Account;
import dev.pmlsp.pixnfc.domain.model.AccountType;
import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.Owner;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.model.PixKeyType;
import dev.pmlsp.pixnfc.domain.policy.CacheTtlPolicy;
import dev.pmlsp.pixnfc.infrastructure.config.PixNfcProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RegulatoryCacheTtlPolicyTest {

    private static final Account ACC = new Account(Ispb.of("12345678"), "0001", "111", AccountType.CACC);
    private static final Owner OWNER = Owner.naturalPerson("12345678901", "Alice");

    private static DictEntry entry(PixKeyType type, String value) {
        Instant now = Instant.now();
        return DictEntry.builder()
                .key(PixKey.of(type, value))
                .account(ACC).owner(OWNER)
                .creationDate(now).keyOwnershipDate(now)
                .build();
    }

    private static PixNfcProperties.Dict.Cache.Ttl ttl(
            Duration cpf, Duration cnpj, Duration email, Duration phone, Duration evp) {
        return new PixNfcProperties.Dict.Cache.Ttl(cpf, cnpj, email, phone, evp);
    }

    @Test
    void honorsConfiguredTtlBelowMax() {
        CacheTtlPolicy policy = new RegulatoryCacheTtlPolicy(
                ttl(Duration.ofSeconds(20), Duration.ofSeconds(60), Duration.ofSeconds(60),
                        Duration.ofSeconds(60), Duration.ofSeconds(30)));

        Duration ttl = policy.ttlFor(entry(PixKeyType.CPF, "12345678901"));

        assertThat(ttl).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void clampsConfiguredTtlAboveRegulatoryMax() {
        CacheTtlPolicy policy = new RegulatoryCacheTtlPolicy(
                ttl(Duration.ofSeconds(30), Duration.ofSeconds(60), Duration.ofSeconds(60),
                        Duration.ofSeconds(60), Duration.ofSeconds(600)));

        Duration ttl = policy.ttlFor(entry(PixKeyType.EVP,
                "550e8400-e29b-41d4-a716-446655440000"));

        assertThat(ttl).isEqualTo(CacheTtlPolicy.MAX_EVP);
    }
}

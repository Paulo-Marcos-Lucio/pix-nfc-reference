package dev.pmlsp.pixnfc.infrastructure.cache;

import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.PixKeyType;
import dev.pmlsp.pixnfc.domain.policy.CacheTtlPolicy;
import dev.pmlsp.pixnfc.infrastructure.config.PixNfcProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Policy padrão de TTL que:
 * <ol>
 *   <li>Honra o TTL configurado em {@code pixnfc.dict.cache.ttl.*}</li>
 *   <li>Faz clamp ao máximo regulatório por tipo de chave</li>
 * </ol>
 * Quando o operador configura TTL acima do permitido, o policy emite warning
 * e usa o teto regulatório — config errada não vira multa.
 */
@Slf4j
public class RegulatoryCacheTtlPolicy implements CacheTtlPolicy {

    private final PixNfcProperties.Dict.Cache.Ttl configured;

    public RegulatoryCacheTtlPolicy(PixNfcProperties.Dict.Cache.Ttl configured) {
        this.configured = configured;
    }

    @Override
    public Duration ttlFor(DictEntry entry) {
        Duration max = maxFor(entry.getKey().type());
        Duration desired = configured.forKey(entry.getKey().type());
        if (desired.compareTo(max) > 0) {
            log.warn("dict.cache.ttl.clamped keyType={} configured={}s max={}s",
                    entry.getKey().type(), desired.toSeconds(), max.toSeconds());
            return max;
        }
        return desired;
    }

    private static Duration maxFor(PixKeyType type) {
        return switch (type) {
            case CPF -> MAX_CPF;
            case CNPJ -> MAX_CNPJ;
            case EMAIL -> MAX_EMAIL;
            case PHONE -> MAX_PHONE;
            case EVP -> MAX_EVP;
        };
    }
}

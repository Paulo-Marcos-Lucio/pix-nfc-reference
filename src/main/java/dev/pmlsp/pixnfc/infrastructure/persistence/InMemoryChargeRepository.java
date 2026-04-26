package dev.pmlsp.pixnfc.infrastructure.persistence;

import dev.pmlsp.pixnfc.domain.model.NfcCharge;
import dev.pmlsp.pixnfc.domain.port.out.ChargeRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório in-memory de cobranças NFC. Default do reference; em produção
 * troca-se por adapter Postgres (com TTL via partição) ou Redis (com EXPIRE).
 */
public class InMemoryChargeRepository implements ChargeRepository {

    private final Map<UUID, NfcCharge> store = new ConcurrentHashMap<>();

    @Override
    public NfcCharge save(NfcCharge charge) {
        store.put(charge.getChargeId(), charge);
        return charge;
    }

    @Override
    public Optional<NfcCharge> findById(UUID chargeId) {
        return Optional.ofNullable(store.get(chargeId));
    }
}

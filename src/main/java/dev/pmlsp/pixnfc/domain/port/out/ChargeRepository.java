package dev.pmlsp.pixnfc.domain.port.out;

import dev.pmlsp.pixnfc.domain.model.NfcCharge;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de cobranças NFC do lado recebedor. Implementação default é
 * in-memory; em produção é tipicamente Postgres ou Redis com TTL.
 */
public interface ChargeRepository {

    NfcCharge save(NfcCharge charge);

    Optional<NfcCharge> findById(UUID chargeId);
}

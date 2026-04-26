package dev.pmlsp.pixnfc.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Representação cacheable de uma entrada DICT — chave Pix mapeada para
 * conta e titular. Usada durante validação de payload NFC para confirmar
 * que a chave declarada no payload pertence ao ISPB declarado.
 */
@Value
@Builder
public class DictEntry {

    PixKey key;
    Account account;
    Owner owner;
    Instant keyOwnershipDate;
    Instant creationDate;
}

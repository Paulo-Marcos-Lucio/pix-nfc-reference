package dev.pmlsp.pixnfc.domain.port.out;

import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.PixKey;

import java.util.Optional;

/**
 * Output port abstracting the BCB DICT lookup API. The Pix-NFC reference
 * uses DICT only to resolve the merchant key declared in an NFC payload —
 * full DICT operations (claim, write, infraction) live in the
 * companion {@code dict-client-reference} project.
 */
public interface DictGateway {

    Optional<DictEntry> lookup(PixKey key, Ispb requesterIspb);
}

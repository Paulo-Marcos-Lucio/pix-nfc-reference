package dev.pmlsp.pixnfc.domain.port.in;

import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.PixKey;

public interface LookupKeyUseCase {

    DictEntry lookup(LookupQuery query);

    record LookupQuery(PixKey key, Ispb payerIspb) {}
}

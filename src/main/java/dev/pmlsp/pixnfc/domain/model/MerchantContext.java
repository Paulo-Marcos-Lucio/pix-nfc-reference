package dev.pmlsp.pixnfc.domain.model;

/**
 * Identidade do recebedor desta instância. Imutável e parte do domain
 * pra que use cases não dependam de classes de infrastructure (Spring properties).
 */
public record MerchantContext(Ispb ispb, PixKey pixKey, String displayName) {

    public MerchantContext {
        java.util.Objects.requireNonNull(ispb, "ispb");
        java.util.Objects.requireNonNull(pixKey, "pixKey");
    }
}

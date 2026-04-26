package dev.pmlsp.pixnfc.infrastructure.config;

import dev.pmlsp.pixnfc.domain.model.PixKeyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Top-level binding for {@code pixnfc.*} configuration. Java records make
 * each field constructor-bound and immutable after startup.
 */
@ConfigurationProperties(prefix = "pixnfc")
public record PixNfcProperties(
        Merchant merchant,
        Crypto crypto,
        Dict dict,
        Spi spi,
        Mtls mtls,
        Simulator simulator) {

    /** Identidade do recebedor desta instância (PSP recebedor). */
    public record Merchant(String ispb, String pixKey, String displayName) {}

    /** HMAC settings para assinatura de payload NFC. */
    public record Crypto(String hmacKey, String hmacAlgorithm) {}

    /** DICT (consulta de chaves do recebedor durante validação). */
    public record Dict(
            Endpoint endpoint,
            Cache cache) {

        public record Endpoint(
                String baseUrl,
                Duration connectTimeout,
                Duration readTimeout) {}

        public record Cache(int maxSize, Ttl ttl) {

            public record Ttl(
                    Duration cpf,
                    Duration cnpj,
                    Duration email,
                    Duration phone,
                    Duration evp) {

                public Duration forKey(PixKeyType type) {
                    return switch (type) {
                        case CPF -> cpf;
                        case CNPJ -> cnpj;
                        case EMAIL -> email;
                        case PHONE -> phone;
                        case EVP -> evp;
                    };
                }
            }
        }
    }

    /** SPI (canal de liquidação Pix do BCB). */
    public record Spi(Endpoint endpoint) {

        public record Endpoint(
                String baseUrl,
                Duration connectTimeout,
                Duration readTimeout) {}
    }

    /** mTLS comum a DICT e SPI (ambos exigem ICP-Brasil em produção). */
    public record Mtls(boolean enabled, String bundleName) {}

    /** Simulator embutido pra dev/test sem cert real. */
    public record Simulator(
            boolean enabled,
            double failureRate,
            Duration latencyJitter,
            int storeCapacity) {}

}

package dev.pmlsp.pixnfc.infrastructure.http;

import dev.pmlsp.pixnfc.infrastructure.config.PixNfcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Constrói o {@link RestClient} usado pelo gateway do SPI.
 * Mesmo material mTLS do DICT — em produção ambos exigem ICP-Brasil.
 */
@Slf4j
public final class SpiHttpClientFactory {

    private final PixNfcProperties props;
    private final SslBundles sslBundles;

    public SpiHttpClientFactory(PixNfcProperties props, SslBundles sslBundles) {
        this.props = props;
        this.sslBundles = sslBundles;
    }

    public RestClient build() {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(props.spi().endpoint().connectTimeout())
                .withReadTimeout(props.spi().endpoint().readTimeout());

        if (props.mtls().enabled() && props.mtls().bundleName() != null) {
            SslBundle bundle = sslBundles.getBundle(props.mtls().bundleName());
            settings = settings.withSslBundle(bundle);
            log.info("spi.http.mtls enabled bundle={}", props.mtls().bundleName());
        } else {
            log.warn("spi.http.mtls disabled — only acceptable for local/simulator profiles");
        }

        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.jdk().build(settings);

        return RestClient.builder()
                .baseUrl(props.spi().endpoint().baseUrl())
                .requestFactory(factory)
                .build();
    }
}

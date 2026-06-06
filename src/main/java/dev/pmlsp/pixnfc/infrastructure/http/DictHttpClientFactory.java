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
 * Constrói o {@link RestClient} usado pelo {@link DictHttpGateway}.
 *
 * <p>Se {@code pixnfc.mtls.enabled=true} e existe bundle configurado, o {@link SslBundle}
 * resolvido pelo Spring Boot é injetado — keystore (cert participante) e truststore
 * (cadeia ICP-Brasil) vêm de {@code spring.ssl.bundle.*}.
 */
@Slf4j
public final class DictHttpClientFactory {

    private final PixNfcProperties props;
    private final SslBundles sslBundles;

    public DictHttpClientFactory(PixNfcProperties props, SslBundles sslBundles) {
        this.props = props;
        this.sslBundles = sslBundles;
    }

    public RestClient build() {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(props.dict().endpoint().connectTimeout())
                .withReadTimeout(props.dict().endpoint().readTimeout());

        if (props.mtls().enabled() && props.mtls().bundleName() != null) {
            SslBundle bundle = sslBundles.getBundle(props.mtls().bundleName());
            settings = settings.withSslBundle(bundle);
            log.info("dict.http.mtls enabled bundle={}", props.mtls().bundleName());
        } else {
            log.warn("dict.http.mtls disabled — only acceptable for local/simulator profiles");
        }

        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.jdk().build(settings);

        return RestClient.builder()
                .baseUrl(props.dict().endpoint().baseUrl())
                .requestFactory(factory)
                .build();
    }
}

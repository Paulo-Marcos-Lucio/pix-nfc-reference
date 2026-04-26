package dev.pmlsp.pixnfc.infrastructure.config;

import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.MerchantContext;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.policy.CacheTtlPolicy;
import dev.pmlsp.pixnfc.domain.port.out.ChargeRepository;
import dev.pmlsp.pixnfc.domain.port.out.DictEntryCache;
import dev.pmlsp.pixnfc.domain.port.out.DictGateway;
import dev.pmlsp.pixnfc.domain.port.out.PayloadCodec;
import dev.pmlsp.pixnfc.domain.port.out.PixSpiGateway;
import dev.pmlsp.pixnfc.infrastructure.cache.CaffeineDictEntryCache;
import dev.pmlsp.pixnfc.infrastructure.cache.RegulatoryCacheTtlPolicy;
import dev.pmlsp.pixnfc.infrastructure.crypto.HmacPayloadCodec;
import dev.pmlsp.pixnfc.infrastructure.http.DictHttpClientFactory;
import dev.pmlsp.pixnfc.infrastructure.http.DictHttpGateway;
import dev.pmlsp.pixnfc.infrastructure.http.SpiHttpClientFactory;
import dev.pmlsp.pixnfc.infrastructure.http.SpiHttpGateway;
import dev.pmlsp.pixnfc.infrastructure.persistence.InMemoryChargeRepository;
import dev.pmlsp.pixnfc.infrastructure.simulator.InMemoryDictStore;
import dev.pmlsp.pixnfc.infrastructure.simulator.SimulatorBehavior;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * Wire dos beans de infrastructure que dependem de {@link PixNfcProperties}.
 * Mantido enxuto: tudo que é Spring-specific vive aqui, fora dos use cases.
 */
@Configuration
public class PixNfcAutoConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    MerchantContext merchantContext(PixNfcProperties props) {
        return new MerchantContext(
                Ispb.of(props.merchant().ispb()),
                PixKey.parse(props.merchant().pixKey()),
                props.merchant().displayName());
    }

    @Bean
    CacheTtlPolicy cacheTtlPolicy(PixNfcProperties props) {
        return new RegulatoryCacheTtlPolicy(props.dict().cache().ttl());
    }

    @Bean
    DictEntryCache dictEntryCache(PixNfcProperties props, MeterRegistry registry) {
        return new CaffeineDictEntryCache(props.dict().cache().maxSize(), registry);
    }

    @Bean
    RestClient dictRestClient(PixNfcProperties props, SslBundles sslBundles) {
        return new DictHttpClientFactory(props, sslBundles).build();
    }

    @Bean
    DictGateway dictGateway(RestClient dictRestClient) {
        return new DictHttpGateway(dictRestClient);
    }

    @Bean
    RestClient spiRestClient(PixNfcProperties props, SslBundles sslBundles) {
        return new SpiHttpClientFactory(props, sslBundles).build();
    }

    @Bean
    PixSpiGateway pixSpiGateway(RestClient spiRestClient) {
        return new SpiHttpGateway(spiRestClient);
    }

    @Bean
    PayloadCodec payloadCodec(PixNfcProperties props, Clock clock) {
        return new HmacPayloadCodec(
                props.crypto().hmacKey(),
                props.crypto().hmacAlgorithm(),
                clock);
    }

    @Bean
    ChargeRepository chargeRepository() {
        return new InMemoryChargeRepository();
    }

    @Bean
    @Profile("simulator")
    InMemoryDictStore inMemoryDictStore() {
        return new InMemoryDictStore();
    }

    @Bean
    @Profile("simulator")
    SimulatorBehavior simulatorBehavior(PixNfcProperties props, MeterRegistry registry) {
        return new SimulatorBehavior(props.simulator(), registry);
    }
}

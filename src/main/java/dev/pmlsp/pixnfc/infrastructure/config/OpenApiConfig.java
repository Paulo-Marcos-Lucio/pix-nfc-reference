package dev.pmlsp.pixnfc.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI pixNfcOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Pix NFC Reference API")
                .description("Reference Java implementation of Pix por aproximação (NFC) — emit charge, validate payload, process payment via SPI")
                .version("0.1.0")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}

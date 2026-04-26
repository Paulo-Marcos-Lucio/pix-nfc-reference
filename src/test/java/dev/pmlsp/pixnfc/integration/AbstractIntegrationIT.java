package dev.pmlsp.pixnfc.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Sobe a app com o profile {@code simulator} para que tanto {@code DictSimulatorController}
 * quanto {@code SpiSimulatorController} rodem no mesmo contexto. Os dois gateways HTTP
 * apontam para a mesma porta ephemeral que o Spring Boot bindou.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "simulator"})
public abstract class AbstractIntegrationIT {

    private static final int PORT;

    static {
        try (ServerSocket socket = new ServerSocket(0)) {
            PORT = socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("could not allocate ephemeral port", e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("pixnfc.dict.endpoint.base-url", () -> "http://localhost:" + PORT + "/dict/v1");
        registry.add("pixnfc.spi.endpoint.base-url", () -> "http://localhost:" + PORT + "/spi/v1");
    }

    protected int port() {
        return PORT;
    }
}

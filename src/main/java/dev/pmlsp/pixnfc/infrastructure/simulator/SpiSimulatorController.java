package dev.pmlsp.pixnfc.infrastructure.simulator;

import dev.pmlsp.pixnfc.infrastructure.http.dto.HttpDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Simulator in-process do SPI (Sistema de Pagamentos Instantâneos do BCB).
 * Implementa o contrato que {@link dev.pmlsp.pixnfc.infrastructure.http.SpiHttpGateway}
 * consome. Ativo apenas com profile {@code simulator}. Ouvinte em {@code /spi/v1}.
 *
 * <p>Reproduz o comportamento básico de liquidação NFC: aceita request, gera
 * endToEndId no formato ISO 20022, devolve {@code settled=true}. Pode injetar
 * falhas e latência via {@link SimulatorBehavior}.
 */
@Slf4j
@Profile("simulator")
@RestController
@RequestMapping("/spi/v1")
@RequiredArgsConstructor
public class SpiSimulatorController {

    private final SimulatorBehavior behavior;
    private final SecureRandom random = new SecureRandom();

    @PostMapping("/payments/nfc")
    public ResponseEntity<?> settle(@RequestBody HttpDtos.SpiSettleRequest req) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();

        String endToEndId = generateEndToEndId(req.payerAccount().ispb());
        Instant settledAt = Instant.now();

        log.info("spi.simulator.settled chargeId={} endToEndId={} amountCents={}",
                req.chargeId(), endToEndId, req.amountCents());

        HttpDtos.SpiSettleResponse resp = new HttpDtos.SpiSettleResponse(
                true, endToEndId, settledAt, null);
        return ResponseEntity.ok(resp);
    }

    private String generateEndToEndId(String payerIspb) {
        String ts = String.valueOf(System.currentTimeMillis());
        byte[] random8 = new byte[8];
        random.nextBytes(random8);
        String suffix = HexFormat.of().formatHex(random8).toUpperCase();
        // ISO 20022: E + 8-digit ISPB + variable timestamp + suffix, padded to 32
        String raw = "E" + payerIspb + ts + suffix;
        if (raw.length() >= 32) return raw.substring(0, 32);
        return (raw + "0".repeat(32 - raw.length()));
    }

    private ResponseEntity<?> fail() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HttpDtos.SpiSettleResponse(false, null, null, "SIMULATED_FAILURE"));
    }
}

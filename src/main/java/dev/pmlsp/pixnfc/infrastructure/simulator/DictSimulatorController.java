package dev.pmlsp.pixnfc.infrastructure.simulator;

import dev.pmlsp.pixnfc.infrastructure.http.dto.HttpDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulator in-process do DICT do BCB — implementa o contrato de lookup que
 * o {@link dev.pmlsp.pixnfc.infrastructure.http.DictHttpGateway} consome.
 * Ativo apenas com profile {@code simulator}. Ouvinte em {@code /dict/v1}.
 *
 * <p>Apenas operação de lookup é simulada — o repo {@code dict-client-reference}
 * cobre o conjunto completo de operações DICT.
 */
@Slf4j
@Profile("simulator")
@RestController
@RequestMapping("/dict/v1")
@RequiredArgsConstructor
public class DictSimulatorController {

    private final InMemoryDictStore store;
    private final SimulatorBehavior behavior;

    @GetMapping("/entries/{type}/{value}")
    public ResponseEntity<?> lookup(
            @PathVariable String type,
            @PathVariable String value,
            @RequestParam(value = "payerIspb", required = false) String payerIspb) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        return store.getEntry(type, value)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HttpDtos.ProblemPayload("KEY_NOT_FOUND", "no DICT entry for given key")));
    }

    private ResponseEntity<?> fail() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HttpDtos.ProblemPayload("SIMULATED_FAILURE", "simulator injected error"));
    }
}

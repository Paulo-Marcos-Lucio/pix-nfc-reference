package dev.pmlsp.pixnfc.infrastructure.simulator;

import dev.pmlsp.pixnfc.infrastructure.config.PixNfcProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Encapsulates the misbehaviors a developer can dial into the simulator to exercise
 * client-side resilience: error injection and latency jitter.
 *
 * <p>Emits {@code dict.simulator.failures.injected} every time {@link #shouldFail()}
 * returns true so dashboards can correlate Resilience4j retries / circuit-breaker
 * trips with the synthetic load.
 */
@Slf4j
public class SimulatorBehavior {

    private final PixNfcProperties.Simulator props;
    private final Counter injectedFailures;
    private final Counter appliedJitter;

    public SimulatorBehavior(PixNfcProperties.Simulator props, MeterRegistry registry) {
        this.props = props;
        this.injectedFailures = registry == null ? null
                : Counter.builder("dict.simulator.failures.injected")
                        .description("Number of synthetic 5xx responses injected by the simulator")
                        .register(registry);
        this.appliedJitter = registry == null ? null
                : Counter.builder("dict.simulator.jitter.applied")
                        .description("Number of requests that received synthetic latency jitter")
                        .register(registry);
    }

    /**
     * @return {@code true} when the simulator should pretend to fail the next request.
     */
    public boolean shouldFail() {
        if (props == null || props.failureRate() <= 0) return false;
        boolean fail = ThreadLocalRandom.current().nextDouble() < props.failureRate();
        if (fail && injectedFailures != null) {
            injectedFailures.increment();
        }
        return fail;
    }

    /**
     * Sleep a random duration up to {@code latencyJitter} to mimic real network jitter.
     */
    public void applyLatencyJitter() {
        if (props == null || props.latencyJitter() == null || props.latencyJitter().isZero()) return;
        long maxMs = props.latencyJitter().toMillis();
        if (maxMs <= 0) return;
        long sleep = ThreadLocalRandom.current().nextLong(maxMs);
        if (appliedJitter != null) appliedJitter.increment();
        try {
            Thread.sleep(Duration.ofMillis(sleep));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

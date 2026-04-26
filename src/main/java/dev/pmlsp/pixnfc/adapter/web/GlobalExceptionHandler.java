package dev.pmlsp.pixnfc.adapter.web;

import dev.pmlsp.pixnfc.domain.exception.ChargeNotFoundException;
import dev.pmlsp.pixnfc.domain.exception.DictException;
import dev.pmlsp.pixnfc.domain.exception.InvalidPayloadException;
import dev.pmlsp.pixnfc.domain.exception.KeyNotFoundException;
import dev.pmlsp.pixnfc.domain.exception.PolicyViolationException;
import dev.pmlsp.pixnfc.domain.exception.RateLimitedException;
import dev.pmlsp.pixnfc.domain.exception.SpiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://pix-nfc-reference/problems/";

    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<ProblemDetail> handle(InvalidPayloadException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.reasonCode(), ex.getMessage());
    }

    @ExceptionHandler(ChargeNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(ChargeNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "CHARGE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(KeyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(KeyNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "KEY_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ProblemDetail> handle(RateLimitedException ex) {
        HttpHeaders headers = new HttpHeaders();
        ex.retryAfter().ifPresent(d -> headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(d.toSeconds())));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(buildBody(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ex.getMessage()));
    }

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ProblemDetail> handle(PolicyViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage());
    }

    @ExceptionHandler(SpiUnavailableException.class)
    public ResponseEntity<ProblemDetail> handle(SpiUnavailableException ex) {
        log.warn("spi.upstream.unavailable: {}", ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "UPSTREAM_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(DictException.class)
    public ResponseEntity<ProblemDetail> handleDictFallback(DictException ex) {
        log.warn("pixnfc.unhandled: {}", ex.getMessage());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("validation failed");
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handle(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(buildBody(status, code, detail));
    }

    private static ProblemDetail buildBody(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + code.toLowerCase().replace('_', '-')));
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        return pd;
    }
}

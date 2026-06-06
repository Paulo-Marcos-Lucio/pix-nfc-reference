# Changelog

Todas as mudanças relevantes deste projeto são documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o versionamento segue [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Unreleased]

### Changed — Manutenção junho/2026

- **Migração para Spring Boot 4.0.6** (Framework 7, Jakarta EE 11, Tomcat 11) a partir do 3.4.1.
  - HTTP client factories migrados para a nova API: `ClientHttpRequestFactories` → `ClientHttpRequestFactoryBuilder.jdk()` e `ClientHttpRequestFactorySettings` → `HttpClientSettings` (módulo `spring-boot-http-client`, agora dependência explícita pois não vem transitivo do starter-web no Boot 4).
  - `NfcEndToEndIT` reescrito de `TestRestTemplate` (removido no Boot 4) para `RestClient` — mesmo cenário end-to-end, sem test-support extra.
- **resilience4j** 2.2.0 → 2.4.0, com troca do módulo `resilience4j-spring-boot3` → `resilience4j-spring-boot4` (compatível com Framework 7).
- **springdoc-openapi** 2.7.0 → 3.0.3 (linha 3.x exigida pelo Boot 4).
- **logstash-logback-encoder** 8.0 → 9.0.
- **lombok** 1.18.36 → 1.18.46.
- **archunit** 1.3.0 → 1.4.2.
- **wiremock-standalone** 3.10.0 → 3.13.2.
- **testcontainers** 1.20.4 → 1.21.4.
- **jacoco-maven-plugin** 0.8.12 → 0.8.15.
- **GitHub Actions** atualizadas em todos os workflows (checkout v6, setup-java v5, codeql v4, upload/download-artifact v7/v8, codecov v6, trivy/docker/gh-release majors).

## [0.1.0] - 2026-04-26

Primeira release pública. Implementação Java de referência para **Pix por Aproximação (NFC)**, com simuladores DICT e SPI rodando junto.

### Added — Domínio NFC

- `NfcPayload` imutável com chargeId, ISPB do recebedor, chave, valor, terminal, label, janela de validade e assinatura
- `NfcCharge` com state machine (`PENDING → CONFIRMED|EXPIRED|FAILED`)
- `Reason` codes pra falhas estruturadas
- `PayloadCodec` (port) com implementação `HmacPayloadCodec` HMAC-SHA256

### Added — Use cases

- `IssueChargeService` — lado recebedor: gera payload assinado e persiste cobrança PENDING
- `ValidatePayloadService` — lado pagador: verify HMAC + checa expiração + cross-check via DICT
- `ProcessPaymentService` — lado pagador: orquestra validate + settle via SPI

### Added — Infrastructure

- HTTP clients Spring `RestClient` com **mTLS configurável** via `SslBundle` (DICT e SPI)
- `HmacPayloadCodec` com constant-time compare anti-timing-attack, encode base64 wire format `BR1`
- `RegulatoryCacheTtlPolicy` faz clamp do TTL ao máximo BCB por keyType e loga warning
- `CaffeineDictEntryCache` in-process com gauges `dict.cache.size` e `dict.cache.max_size`
- Resilience4j por grupo: `dict-lookup` (rate limited + retry agressivo + CB), `spi-pay` (retry conservador + CB sensível)
- Audit log estruturado JSON com PII mascarada
- W3C Trace Context cross-service via OpenTelemetry

### Added — Simulator

- `DictSimulatorController` (`/dict/v1`) implementa lookup com 5 chaves seed
- `SpiSimulatorController` (`/spi/v1`) implementa settle gerando endToEndId ISO 20022
- `SimulatorBehavior` injeta latency jitter e taxa de erro pra exercitar resilience client-side
- Ambos sob `@Profile("simulator")` — não vão pra produção

### Added — Web facade

- `POST /v1/nfc/charges` — emitir cobrança
- `POST /v1/nfc/payments/validate` — validar payload sem cobrar
- `POST /v1/nfc/payments` — autorizar e disparar Pix
- `GlobalExceptionHandler` com `application/problem+json` (RFC 7807)

### Added — Observabilidade

- 2 dashboards Grafana versionados como código (`nfc-overview`, `nfc-resilience`)
- Auto-provisioning de datasources (Prometheus, Tempo, Loki)
- Histogram `nfc.operation.duration` com SLO buckets [50ms, 100ms, 250ms, 500ms, 1s] + exemplars Tempo

### Added — Qualidade & CI

- ArchUnit valida hexagonal (domain sem Spring/Jakarta, application só em domain)
- 7 jobs CI paralelos (build, unit, IT, ArchUnit, Semgrep, Trivy, Docker)
- CodeQL Java analysis
- Dependency Review em PRs
- Release tag-driven com SBOM CycloneDX e Trivy SARIF

### Documentação

- 5 ADRs (`docs/adr/`)
- BCB compliance mapping (`docs/compliance/bcb-nfc-mapping.md`)

[Unreleased]: https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference/releases/tag/v0.1.0

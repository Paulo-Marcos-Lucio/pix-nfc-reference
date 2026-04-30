[![Suíte Regulatória BR](https://img.shields.io/badge/%F0%9F%8C%90%20Su%C3%ADte%20Regulat%C3%B3ria%20BR-paulo--marcos--lucio.github.io-3fb950?style=for-the-badge)](https://paulo-marcos-lucio.github.io)

# pix-nfc-reference

> Implementação de **referência** *production-grade* do **Pix por Aproximação (NFC)** —
> a modalidade de pagamento Pix mais recente do BCB, em rollout 2025/2026. Demonstra
> emissão e validação criptográfica de payloads, integração com DICT pra resolução de
> chave do recebedor, gateway SPI, mTLS ICP-Brasil-ready, simulador in-process,
> resilience tipada e observabilidade end-to-end versionada como código.
>
> Java 21 · Spring Boot 3.4 · Hexagonal · Resilience4j · Caffeine · OpenTelemetry · Grafana

[![CI](https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference/actions/workflows/ci.yml/badge.svg)](https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference/actions/workflows/codeql.yml/badge.svg)](https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference/actions/workflows/codeql.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)

---

## Por que este repo existe

O **Pix por Aproximação (NFC)** é a expansão mais recente do ecossistema Pix —
permite ao pagador encostar o celular numa maquininha (mesma UX de Apple Pay/Google Pay)
e disparar uma transação Pix sem QR Code. A spec do BCB é nova, e há **pouquíssimo
material open source de referência em Java** ainda. Este repo cobre a lacuna com
nível de produção.

Faz parte da **Suíte de Referência Regulatória BR** mantida ao lado de:

- [`pix-automatico-reference`](https://github.com/Paulo-Marcos-Lucio/pix-automatico-reference) — Pix Automático + Open Finance Fase 4
- [`dict-client-reference`](https://github.com/Paulo-Marcos-Lucio/dict-client-reference) — cliente DICT do BCB

## O que está aqui

- **Lado recebedor:** geração de payload NFC assinado (HMAC-SHA256), persistência de cobrança, formato wire compacto
- **Lado pagador:** decode de payload, verificação criptográfica constant-time, validação de expiração, **cross-check via DICT** (chave declarada bate com ISPB declarado), liquidação via SPI
- **Codec de payload** — formato `BR1|chargeId|merchantIspb|keyType:keyValue|amount|terminalId|label|issuedAt|validitySec|signature` em base64
- **Simuladores in-process** de DICT *e* SPI ativáveis por `@Profile("simulator")` — funciona end-to-end sem cert real
- **Resilience4j** em 2 grupos: `dict-lookup` (rate-limited + retry agressivo + circuit breaker) e `spi-pay` (retry conservador, evita duplicação de cobrança)
- **mTLS ICP-Brasil-ready** via Spring Boot SSL Bundle (DICT e SPI compartilham a cadeia)
- **Audit log estruturado JSON** com chave Pix sempre mascarada
- **Observabilidade rica:** Prometheus + Tempo + Loki, **2 dashboards Grafana versionados como código** (operação + resilience), exemplars Tempo, W3C Trace Context propagado
- **Hexagonal validado por ArchUnit** — domain puro, application apenas em domain, infrastructure implementando ports out
- **CI: 7 jobs paralelos** (build, unit, IT, ArchUnit, Semgrep, Trivy, Docker) + CodeQL + Dependency Review + Release tag-driven

## Quickstart

```bash
# 1. clona
git clone https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference
cd pix-nfc-reference

# 2. sobe stack de observabilidade (Grafana, Prometheus, Tempo, Loki)
make up

# 3. roda app com simulador embutido (DICT + SPI in-process)
make run-sim

# 4. dispara fluxo end-to-end completo
make load
```

Acesse:
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **Grafana (admin/admin):** http://localhost:3000
  - Dashboard **NFC · Operations Overview**
  - Dashboard **NFC · Resilience**
- **Prometheus:** http://localhost:9090

## Endpoints principais

```http
POST /v1/nfc/charges               # lado recebedor: gera payload assinado pra emitir via NFC
POST /v1/nfc/payments/validate     # lado pagador: valida payload sem cobrar
POST /v1/nfc/payments              # lado pagador: valida + dispara Pix via SPI
```

Veja [`requests.http`](./requests.http) pra exemplos rodáveis na IntelliJ ou via VS Code REST Client.

## Arquitetura

Camadas (validadas por **ArchUnit**):

- `domain/` — modelos, exceções, ports. **Sem Spring, Jakarta ou nada externo**
- `application/` — use cases. Depende **apenas** de `domain/`
- `infrastructure/` — implementação dos ports out (HTTP, cache, persistence, crypto, audit, simulator)
- `adapter/web/` — controllers HTTP, DTOs, exception handler

## Padrões de implementação destacados

| Padrão | Onde aplica | Por que importa |
|---|---|---|
| **HMAC constant-time compare** | `HmacPayloadCodec.verify` | Evita timing attack na verificação de assinatura |
| **Cache TTL clamp regulatório** | `RegulatoryCacheTtlPolicy` | Config errada não vira multa — TTL acima do teto BCB é clampeado e logado warning |
| **Cross-check DICT na validação** | `ValidatePayloadService` | Defesa contra payload forjado — chave declarada precisa bater com ISPB no DICT |
| **PII mascarada por default** | `PixKey.masked()`, `Owner.maskedDocument()` | Logs/audit nunca expõem CPF/chave completa |
| **Resilience por grupo de operação** | `dict-lookup` vs `spi-pay` | DICT lookup tolera retry agressivo; SPI pay não (risco de duplicar Pix) |
| **Simulator-as-profile** | `@Profile("simulator")` | IT end-to-end sem cert real; mesmo contrato HTTP do BCB |
| **Hexagonal estrito + ArchUnit** | `HexagonalArchitectureTest` | Garantido em CI — não dá pra acidentalmente importar Spring no domain |
| **W3C Trace Context** | OpenTelemetry + Tempo | Correlation cross-service via `traceparent` header |
| **Sender-constrained mTLS** *(produção)* | Spring SSL Bundle | Token só vale com o cert que o pegou — defesa em profundidade |

## Configuração

Todos os parâmetros via `pixnfc.*` em `application.yml` ou env vars correspondentes.

```yaml
pixnfc:
  merchant:
    ispb: 12345678              # ISPB do PSP recebedor
    pix-key: 12345678000199     # chave do recebedor (auto-detecta tipo)
    display-name: "Loja XYZ"
  crypto:
    hmac-key: ${HMAC_KEY}       # chave HMAC pra assinar payloads (>= 32 bytes em prod)
    hmac-algorithm: HmacSHA256
  dict:
    endpoint:
      base-url: ${DICT_BASE_URL}
      connect-timeout: 2s
      read-timeout: 5s
    cache:
      max-size: 50000
      ttl:                      # clampeado ao máximo BCB por keyType
        cpf: 30s
        cnpj: 60s
        email: 60s
        phone: 60s
        evp: 30s
  spi:
    endpoint:
      base-url: ${SPI_BASE_URL}
      connect-timeout: 2s
      read-timeout: 5s
  mtls:
    enabled: true
    bundle-name: pixnfc-prod    # corresponde ao spring.ssl.bundle.jks.pixnfc-prod
```

## Observabilidade

### Métricas customizadas além das padrão Spring

- `nfc.charge.issued{merchant_ispb}` — counter de cobranças emitidas
- `nfc.payload.validated{outcome}` — counter de validações com outcome
- `nfc.payment.settled{outcome}` — counter de pagamentos com outcome
- `nfc.operation.duration{operation}` — histogram com SLO buckets e exemplars Tempo
- `dict.cache.size`, `dict.cache.max_size` — gauges
- `dict.simulator.failures.injected`, `dict.simulator.jitter.applied` — counters
- `spi.simulator.settled` — counter

### Dashboards versionados

- **NFC · Operations Overview** (`config/grafana/dashboards/nfc-overview.json`):
  cards de throughput, latência p50/p95/p99, taxa de erro, hit rate de cache DICT
- **NFC · Resilience** (`config/grafana/dashboards/nfc-resilience.json`):
  estado dos circuit breakers (`dict-lookup`, `spi-pay`), retry attempts,
  rate limiter, JVM heap, threads

Auto-provisionados via `config/grafana/provisioning/`.

## Compliance

`docs/compliance/bcb-nfc-mapping.md` — mapping de cada feature do reference
para a seção correspondente do manual do BCB sobre Pix por aproximação.

ADRs em `docs/adr/`:

- [0001 — Hexagonal architecture](docs/adr/0001-hexagonal-architecture.md)
- [0002 — Payload format BR1](docs/adr/0002-payload-format-br1.md)
- [0003 — HMAC vs ECDSA assinatura](docs/adr/0003-hmac-vs-ecdsa.md)
- [0004 — Resilience strategy por grupo](docs/adr/0004-resilience-strategy.md)
- [0005 — Simulator como Spring profile](docs/adr/0005-simulator-as-profile.md)

## Test Coverage & API Docs

| Categoria | Tests |
|---|---|
| Unit (incl. ArchUnit) | 17 |
| Integration (dual simulator) | 1 |
| **Total** | **18** |

JaCoCo coverage report gerado em `target/site/jacoco/index.html` após `./mvnw verify`.

**API Documentation** (live com a app rodando em `http://localhost:8081`):
- Swagger UI: <http://localhost:8081/swagger-ui.html>
- OpenAPI 3 spec (JSON): <http://localhost:8081/v3/api-docs>
- Geração offline do spec:
  ```bash
  ./mvnw spring-boot:run    # em outro terminal
  curl http://localhost:8081/v3/api-docs > docs/openapi.json
  ```

## Roadmap

- [ ] v0.2.0 — substituir HMAC por **ECDSA com cert ICP-Brasil** (assinatura assimétrica de produção)
- [ ] v0.3.0 — adapter ISO 20022 (`pacs.008`) para SPI real, aposentando JSON simplificado do reference
- [ ] v0.4.0 — replicar contract test contra a conformance suite oficial do BCB quando publicada
- [ ] Persistência durável (Postgres com partitioning por dia + TTL automático)
- [ ] Idempotência forte com `Idempotency-Key` em todos os POSTs

## Licença

[MIT](LICENSE) — use, modifique, distribua. Atribuição apreciada.

## Autor

**Paulo Marcos Lucio** — Engenheiro Java pleno · Consultor em integrações regulatórias BR

[LinkedIn](https://www.linkedin.com/in/paulo-marcos-a07379174/) ·
[GitHub](https://github.com/Paulo-Marcos-Lucio) ·
pmlsp23@gmail.com

> Se este repo ajudou seu time, ⭐ uma star — ajuda outros engineers do nicho a encontrarem.

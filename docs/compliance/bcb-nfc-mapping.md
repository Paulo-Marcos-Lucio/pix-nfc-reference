# BCB Pix NFC — Compliance mapping

Este documento mapeia features deste reference às áreas correspondentes do
ecossistema regulatório do BCB pra Pix por aproximação. **Sempre revalide
contra a versão vigente** do manual oficial antes de homologação.

> ⚠️ Os valores numéricos abaixo (timeouts, TTLs, percentuais) são *defaults
> didáticos* deste reference. Em homologação real, cada um precisa ser
> aferido contra a Resolução BCB e o manual operacional vigentes na data.

## 1. Identificação do recebedor

| Aspecto | Implementação |
|---|---|
| ISPB do recebedor declarado no payload | [`NfcPayload.merchantIspb`](../../src/main/java/dev/pmlsp/pixnfc/domain/model/NfcPayload.java) |
| Chave Pix do recebedor declarada e cross-checada via DICT | [`ValidatePayloadService`](../../src/main/java/dev/pmlsp/pixnfc/application/nfc/ValidatePayloadService.java) |
| Mascaramento de chave em logs/audit | [`PixKey.masked()`](../../src/main/java/dev/pmlsp/pixnfc/domain/model/PixKey.java) |

## 2. Integridade e autenticidade do payload

| Aspecto | Implementação |
|---|---|
| Assinatura criptográfica do payload | [`HmacPayloadCodec.sign`](../../src/main/java/dev/pmlsp/pixnfc/infrastructure/crypto/HmacPayloadCodec.java) (v0.1.0 HMAC, v0.2.0+ ECDSA ICP-Brasil — ver ADR 0003) |
| Verificação constant-time (anti-timing-attack) | `HmacPayloadCodec.verify` usa `MessageDigest.isEqual` |
| Janela de validade do payload | `NfcPayload.isExpired(now)` checada em `ValidatePayloadService` |

## 3. Cache regulatório de DICT

| Regra (manual DICT) | Implementação |
|---|---|
| TTL máximo de cache por chave CPF | [`CacheTtlPolicy.MAX_CPF`](../../src/main/java/dev/pmlsp/pixnfc/domain/policy/CacheTtlPolicy.java) |
| TTL máximo CNPJ | `CacheTtlPolicy.MAX_CNPJ` |
| TTL máximo EMAIL/PHONE | `CacheTtlPolicy.MAX_EMAIL` / `MAX_PHONE` |
| TTL máximo EVP | `CacheTtlPolicy.MAX_EVP` |
| Configuração acima do teto é clampeada e logada | [`RegulatoryCacheTtlPolicy.ttlFor`](../../src/main/java/dev/pmlsp/pixnfc/infrastructure/cache/RegulatoryCacheTtlPolicy.java) |

## 4. Comunicação com DICT e SPI

| Aspecto | Implementação |
|---|---|
| mTLS ICP-Brasil (produção) | [`DictHttpClientFactory`](../../src/main/java/dev/pmlsp/pixnfc/infrastructure/http/DictHttpClientFactory.java) e [`SpiHttpClientFactory`](../../src/main/java/dev/pmlsp/pixnfc/infrastructure/http/SpiHttpClientFactory.java) via Spring SSL Bundle |
| Resilience por grupo de operação | [`application.yml`](../../src/main/resources/application.yml) (instances `dict-lookup` e `spi-pay`) — ver ADR 0004 |
| Audit log estruturado JSON | [`StructuredAuditLog`](../../src/main/java/dev/pmlsp/pixnfc/infrastructure/audit/StructuredAuditLog.java) |

## 5. Trazabilidade

| Aspecto | Implementação |
|---|---|
| Trace ID propagado cross-service (W3C) | OpenTelemetry / `traceparent` header (default) |
| `endToEndId` ISO 20022 retornado pelo SPI | [`SpiHttpGateway.settle`](../../src/main/java/dev/pmlsp/pixnfc/infrastructure/http/SpiHttpGateway.java) |
| Charge ID UUID v4 único por cobrança | [`IssueChargeService`](../../src/main/java/dev/pmlsp/pixnfc/application/nfc/IssueChargeService.java) |

## 6. Gaps reconhecidos vs produção real

| Gap | Status | Tracker |
|---|---|---|
| Codec de payload não-aderente ao manual oficial (usa BR1) | Documentado | ADR 0002, roadmap v0.3.0 |
| Assinatura HMAC simétrica em vez de ECDSA ICP-Brasil | Documentado | ADR 0003, roadmap v0.2.0 |
| Persistência in-memory (sem durabilidade) | Documentado | Roadmap |
| Idempotency-Key não implementada nos POSTs | Documentado | Roadmap |
| Adapter SPI usa JSON em vez de ISO 20022 pacs.008 | Documentado | Roadmap v0.3.0 |

> Cada gap é trackeado por ADR ou linha de roadmap no README — não por TODO solto no código.

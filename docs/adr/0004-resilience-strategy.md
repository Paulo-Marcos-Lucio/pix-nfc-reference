# ADR 0004 — Estratégia de resilience por grupo de operação

**Status:** Accepted · 2026-04-26

## Contexto

A app fala com dois sistemas externos (DICT pra resolver chave, SPI pra liquidar
o Pix). Os perfis de risco são **muito diferentes**:

- **DICT lookup:** alta frequência, idempotente, retry barato
- **SPI pay:** evento financeiro, **não é idempotente do ponto de vista do BCB**
  sem cuidado adicional, retry agressivo causa cobrança duplicada

Aplicar a mesma config de retry/circuit breaker nos dois seria um anti-pattern.

## Decisão

Resilience4j configurado em **dois grupos** com perfis distintos:

| Grupo | Circuit Breaker | Retry | Rate Limiter |
|---|---|---|---|
| `dict-lookup` | sliding 50, failure 50%, open 10s | 3 attempts, 200ms backoff exponencial | 50 req/s |
| `spi-pay` | sliding 30, failure 30%, open 20s | 2 attempts, 500ms backoff exponencial | — |

### Diferenças concretas

- **`dict-lookup`** tolera retry agressivo porque lookup é GET idempotente
- **`spi-pay`** com retry conservador (2 tentativas, espaçadas) — minimiza
  janela de duplicação se a primeira request foi bem-sucedida mas não
  recebemos a confirmação
- **`spi-pay`** sem rate limiter — operações de pagamento não devem ser
  throttled localmente; throttling vem do próprio BCB
- **`dict-lookup`** com circuit breaker mais permissivo (50% threshold)
  porque lookup falhar pontualmente não é catastrófico
- **`spi-pay`** com circuit breaker mais sensível (30% threshold) porque
  falhas no SPI tendem a ser sistêmicas; melhor parar de bater e retornar
  erro rápido pro cliente

## Consequências

- **Anotações Resilience4j ficam em adapters HTTP** (`DictHttpGateway`, `SpiHttpGateway`),
  não em use cases — resilience é responsabilidade de infra
- **Adopters podem ajustar via `application.yml`** sem mudar código
- **Métricas Resilience4j publicam por instância** — dashboard de resilience
  mostra os dois grupos lado a lado, fácil de comparar perfis

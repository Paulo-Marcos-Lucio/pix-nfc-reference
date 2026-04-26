# ADR 0005 — Simulator como Spring Profile in-process

**Status:** Accepted · 2026-04-26

## Contexto

Pra testar o fluxo end-to-end sem cert ICP-Brasil real e sem subir mocks
HTTP separados, precisamos de uma forma rápida de "fingir" que o DICT e o SPI
estão disponíveis. Opções:

1. **WireMock** com mappings JSON
2. **Mockito** apenas em camada Java
3. **Simulator embutido** ativado por Spring Profile

## Decisão

**Simulator embutido como Spring Profile (`@Profile("simulator")`)**.

Implementado via `DictSimulatorController` e `SpiSimulatorController`:
controllers REST que respondem nas mesmas paths que o DICT/SPI reais
(`/dict/v1/...` e `/spi/v1/...`), montados na **mesma instância Spring**
da app principal.

## Por quê esta opção venceu

- **Cliente HTTP de verdade** — `RestClient` real, mTLS bypass, retry real,
  circuit breaker real. Mockito mockaria a camada errada
- **Sem JVM extra** — IT roda em uma única instância, ciclo de feedback rápido
- **Reusável dev/test/CI** — `make run-sim` usa o mesmo simulator que IT
- **Controle de comportamento** via `SimulatorBehavior` — taxa de erro e
  jitter de latência configuráveis pra testar resilience client-side

## Trade-offs aceitos

- **Não substitui contract test** com a conformance suite oficial do BCB —
  v0.4.0 adicionará isso quando publicada
- **Estado é in-memory** — perde tudo no restart, propositalmente
- **Não simula cenários multi-tenant** — adequado pra reference, não pra
  staging multi-PSP

## Consequências

- `infrastructure/simulator/` é gitignored de imagens OCI de produção
  (Spring profile activation excluído via `application-prod.yml`)
- Sample data seedado em `InMemoryDictStore.seed()` — chaves deterministas
  pra os exemplos do `requests.http`
- Latência e taxa de erro injetáveis via `pixnfc.simulator.*` — útil pra
  demos de resilience ("olha o circuit breaker abrindo aqui")

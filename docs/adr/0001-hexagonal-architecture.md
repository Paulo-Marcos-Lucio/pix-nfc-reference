# ADR 0001 — Hexagonal Architecture (Ports & Adapters)

## Status
Accepted

## Context
A DICT client integration must survive several types of change without rewrites:

- **Transport stack**: today HTTP+JSON; the BCB roadmap mentions XML+JWS for some endpoints, and a future move to gRPC over the SPI network is plausible.
- **Security model**: today mTLS via ICP-Brasil; OAuth2 + DPoP, JWE-encrypted payloads or token-binding may follow.
- **Cache substrate**: today Caffeine in-process; multi-replica deployments will want Redis or a near-cache + far-cache combo.
- **Audit destination**: today structured logs; tomorrow may be a regulatory audit table or an external SIEM.

A traditional Controller → Service → Repository layering couples business rules to frameworks and amplifies the cost of each of those changes.

## Decision
Adopt hexagonal architecture (Ports & Adapters):

- **Domain** (`dev.pmlsp.dict.domain`): pure rules, no framework dependency. Models (`PixKey`, `Account`, `Owner`, `DictEntry`, `Claim`), policies (`CacheTtlPolicy`), input ports (use cases) and output ports (`DictGateway`, `DictEntryCache`, `AuditLog`).
- **Application** (`dev.pmlsp.dict.application`): orchestrates use cases. Depends only on `domain`. Uses Spring annotations (`@Service`) but no JPA, no Kafka, no transport-specific imports.
- **Infrastructure** (`dev.pmlsp.dict.infrastructure`): implements the output ports — HTTP gateway, Caffeine cache, structured audit, simulator.
- **Adapter / Web** (`dev.pmlsp.dict.adapter.web`): demo HTTP facade — controllers, DTOs, exception handler. Translates HTTP into use case commands.

`HexagonalArchitectureTest` (ArchUnit) enforces these layers in CI.

## Consequences
### Positive
- Domain unit tests are microseconds (no Spring context, no container).
- Swapping cache (Caffeine → Redis), audit sink, or gateway transport doesn't touch the use cases.
- The simulator is just another `DictGateway` adapter — no special wiring needed in the domain.

### Negative
- More classes per concept: domain model + HTTP DTO + (potentially) JPA entity + mappers. Accepted because the maintenance cost of decoupling later is higher.
- Some boilerplate for ports and use case interfaces. Mitigated by Java records and Lombok.

## Alternatives considered
- **Clean Architecture (Uncle Bob)**: very similar, slightly more formal. Hexagonal is more pragmatic in Spring Boot.
- **Spring's default layered**: simple but leaks framework into the core, defeating the goal.

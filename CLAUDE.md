# Guia para Claude

Notas internas para sessões futuras de desenvolvimento assistido por IA. Não é documentação para usuários — para isso veja [README.md](./README.md) e [CONTRIBUTING.md](./CONTRIBUTING.md).

## O que este projeto é

Implementação Java de **referência** (não app pronto) para Pix por Aproximação (NFC) — modalidade Pix recém-publicada pelo BCB. Posicionamento é portfolio público de consultoria — qualquer mudança deve preservar/elevar o nível profissional do código e da documentação.

Faz parte da **Suíte de Referência Regulatória BR** mantida pelo Paulo SP, ao lado de `pix-automatico-reference` e `dict-client-reference`.

## Convenções inegociáveis

### Arquitetura hexagonal
- `domain/` é **puro**: sem Spring, sem Jakarta, sem nada externo. Só Java + Lombok.
- `application/` depende **apenas** de `domain/`. Nunca importe de `infrastructure/` ou `adapter/`.
- `infrastructure/` implementa portas de saída (`domain/port/out/`).
- `adapter/web/` é o ponto de entrada HTTP — controllers, DTOs, filters.
- `HexagonalArchitectureTest` (ArchUnit) valida tudo isso no CI. Não burlar.

### TTL de cache regulatório (DICT lookup do recebedor)
- O manual do DICT impõe **TTL máximo** de cache por tipo de operação. `CacheTtlPolicy` é a fonte da verdade.
- Não permitir configurar TTL acima do limite — a config sobe, o policy clamp para o teto.
- Chave com claim em aberto **não pode** ser cacheada — `lookup` deve sempre bater no DICT.

### mTLS
- Cliente HTTP usa `SslBundle` resolvido pelo Spring. Truststore aceita só ICP-Brasil em produção; em local/test pode aceitar self-signed.
- Nunca commitar certificados, chaves privadas, p12, jks. `.gitignore` bloqueia.
- Para dev local: `make certs` gera material de teste em `certs/` (gitignored).

### Mascaramento de PII
- Toda chave Pix em log/audit/exception **deve** sair mascarada. Helper `PixKey.masked()` é a forma canônica.
- Nunca logar `pixKey.value()` direto. ArchUnit não pega isso — review humano sim.

### Simulador
- Ativado via `@Profile("simulator")`. Pode rodar no mesmo Spring context da app (modo dev/test) ou em outra instância (modo IT).
- `SimulatorBehavior` permite injetar latência e taxa de erro para testar resiliência client-side.
- Estado é in-memory (`InMemoryDictStore`) — perde tudo no restart, propositalmente.

### Resilience4j
- 2 grupos: `dict-lookup` (alta frequência, rate-limited, retry agressivo) e `spi-pay` (retry conservador, CB sensível — Pix não tolera duplicação).
- Não decorar use cases, decorar adapters HTTP — resilience é responsabilidade de infra.

### Testes
- Unit: `*Test.java` em `src/test/java/...`. Domínio puro testado direto.
- Integration: `*IT.java`, herda de `AbstractIntegrationIT`. Sobe simulator no contexto.
- Architecture: `HexagonalArchitectureTest`.
- Surefire roda `*Test.java` na fase `test`. Failsafe roda `*IT.java` na fase `verify`.

## Comandos frequentes

```bash
# Stack local (observabilidade)
make up
make down

# Build / testes
make test     # unit
make it       # unit + integration

# Run app
make run        # profile local (sem mTLS, gateway HTTP simulado)
make run-sim    # profile local + simulator (cliente DICT contra simulador in-process)

# Imagem OCI via Buildpacks
make image
```

## Operações no GitHub via gh

`gh` é autenticado via env var puxando o PAT do Git Credential Manager:
```bash
export GH_TOKEN=$(printf "protocol=https\nhost=github.com\n\n" | git credential fill 2>/dev/null | sed -n 's/^password=//p' | head -1)
```
Token tem scopes `repo`, `workflow`, `gist`. Suficiente pra tudo no repo.

Comandos úteis:
```bash
gh run list -R Paulo-Marcos-Lucio/pix-nfc-reference --limit 5 -w CI
gh run watch <run-id> -R Paulo-Marcos-Lucio/pix-nfc-reference --exit-status
gh run view --job <job-id> --log -R Paulo-Marcos-Lucio/pix-nfc-reference
```

## Branch protection

`main` é protegida: status checks `CI status` e `Analyze (Java)` obrigatórios, linear history, sem force push, sem delete. Mudanças entram via PR squash-merged.

## Release

Tag `v*.*.*` dispara `release.yml`: builda imagem OCI, push pra `ghcr.io/Paulo-Marcos-Lucio/pix-nfc-reference`, cria GitHub Release com SBOM CycloneDX e Trivy SARIF.

```bash
git tag v0.2.0
git push origin v0.2.0
```

## Fora de escopo (não fazer sem pedido)

- Conexão real com DICT do BCB em produção (precisa certificado ICP-Brasil emitido pra um participante real)
- Persistência durável (audit em DB, claims em DB) — hoje audit é só log estruturado
- Retentativa async via fila (DICT é request/response síncrono)
- Frontend (este projeto é uma lib + facade demo, não tem app de usuário final)

## Mensagens de commit

PT-BR informal, claro e elucidativo. Mantém o prefixo Conventional Commits (`fix(escopo):`, `feat(escopo):`, `chore:`, `test:`, etc.) mas o texto em português. Explica *o que* mudou e *por quê*. Exemplo bom:

```
fix(cache): clamp TTL ao limite BCB mesmo se config pedir mais

Properties permitiam configurar TTL de cache acima do permitido pelo
manual DICT v3.2 (60s para chave de PJ). Em produção isso violava o
contrato regulatório. CacheTtlPolicy agora aplica Math.min(configured,
regulatoryMax) e loga warning quando faz o clamp.
```

## Memória do harness

Arquivos em `~/.claude/projects/.../memory/` documentam preferências do Paulo (autonomia, comunicação, posicionamento profissional, suíte de repos). Atualizar quando aprender algo durável; não duplicar conteúdo do código.

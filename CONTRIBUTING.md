# Contribuindo

Obrigado pelo interesse! Este repositório é uma referência técnica pública — contribuições que melhorem clareza, correção ou aderência ao manual do DICT (BCB) são muito bem-vindas.

## Como começar

1. Faça fork e crie um branch a partir de `main`:
   ```bash
   git checkout -b feat/minha-mudanca
   ```
2. (Opcional) Suba a stack de observabilidade local:
   ```bash
   make up
   ```
3. Rode os testes:
   ```bash
   make it
   ```

## Convenções

### Commits

Seguimos [Conventional Commits](https://www.conventionalcommits.org/pt-br/).

```
<tipo>(<escopo opcional>): <descrição curta no imperativo>

[corpo opcional explicando o porquê, não o quê]

[footer opcional, ex: Closes #123 / BREAKING CHANGE: ...]
```

Tipos aceitos: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `chore`, `ci`, `build`.

Exemplos:
```
feat(lookup): adiciona TTL diferenciado para PESSOA_JURIDICA
fix(mtls): corrige handshake quando truststore tem múltiplas CAs ICP-Brasil
docs(adr): atualiza ADR-0003 com tabela de TTL conforme manual DICT v3.2
```

### Branches

- `main`: protegida, requer PR + CI verde + 1 review de CODEOWNER
- `feat/*`, `fix/*`, `refactor/*`, `chore/*`: branches de trabalho

### Estilo de código

- Java 21 — use `var`, records, pattern matching, virtual threads quando fizer sentido
- Lombok permitido para boilerplate puro (`@Getter`, `@Builder`, `@RequiredArgsConstructor`); evitar `@Data` em modelos
- Domínio (`domain/`) **não pode** importar Spring nem Jakarta — `HexagonalArchitectureTest` valida no CI
- Nomes em português apenas para conceitos do DICT (`Reivindicacao`/`Claim` é a tradução oficial; usamos `Claim`); resto em inglês

### Testes

| Mudança | Testes obrigatórios |
|---|---|
| Regra de domínio | Unitário no agregado |
| Caso de uso (application) | Unitário com ports mockados |
| Adapter de I/O | Integração contra o `simulator` em `@Profile("simulator")` |
| Mudança em controller | Integração HTTP via `@SpringBootTest(webEnvironment = RANDOM_PORT)` |
| Nova camada / pacote | ArchUnit atualizado |

Não use mocks para o gateway HTTP em testes de integração — sempre o **simulador real**.

### ADRs

Decisões arquiteturais não triviais devem virar um ADR em `docs/adr/`. Numere sequencialmente e siga o template das ADRs existentes.

## Fluxo de PR

1. CI precisa estar verde (build, unit, integration, ArchUnit, Semgrep, CodeQL, Trivy)
2. Coverage não pode regredir
3. PR description preenchida pelo template
4. Pelo menos 1 aprovação de CODEOWNER
5. Squash merge é o padrão; merge commit apenas para integração de branches longas

## Reportando vulnerabilidades

**Não abra issue pública.** Veja [SECURITY.md](./SECURITY.md).

## Licença

Ao contribuir, você concorda em licenciar suas mudanças sob a [MIT License](./LICENSE).

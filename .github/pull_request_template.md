<!--
Obrigado por contribuir! Preencha o template para acelerar o review.
Mantenha o PR pequeno e focado num único objetivo.
-->

## Resumo

<!-- O que muda e por quê. 1-3 linhas. -->

## Tipo de mudança

- [ ] feat: nova funcionalidade
- [ ] fix: correção de bug
- [ ] refactor: refatoração sem mudança de comportamento
- [ ] perf: melhoria de performance
- [ ] docs: apenas documentação
- [ ] test: apenas testes
- [ ] chore: build, CI, deps

## Impacto

- [ ] Breaking change (descreva abaixo)
- [ ] Requer mudança de configuração / secrets
- [ ] Afeta contrato externo (HTTP do facade ou contrato com DICT)
- [ ] Afeta política de cache TTL (revisar contra manual BCB)
- [ ] Afeta material criptográfico / mTLS

## Como testar

```bash
# comandos para reproduzir / validar
make it
```

## Checklist

- [ ] Testes unitários cobrindo o caminho feliz e os principais erros
- [ ] Testes de integração quando há I/O HTTP (cliente vs simulator)
- [ ] ArchUnit continua passando (camadas hexagonais respeitadas)
- [ ] PII em logs/audit continua mascarada (chaves Pix nunca em claro)
- [ ] TTL de cache não excede limite BCB
- [ ] Métricas / spans adicionados para operações novas relevantes
- [ ] OpenAPI atualizada se houver mudança no contrato HTTP
- [ ] ADR criado/atualizado se houver decisão arquitetural não trivial
- [ ] CHANGELOG atualizado (seção `[Unreleased]`)

## Issues relacionadas

Closes #

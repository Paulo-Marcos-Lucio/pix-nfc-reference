# ADR 0003 — HMAC vs ECDSA pra assinatura de payload

**Status:** Accepted (com plano de evolução) · 2026-04-26

## Contexto

Payloads NFC precisam ser assinados pra que o app pagador valide que foram
gerados pela maquininha autorizada e não foram adulterados em trânsito.

Duas alternativas naturais:

1. **HMAC-SHA256** — chave simétrica compartilhada entre maquininha e backend recebedor
2. **ECDSA com cert ICP-Brasil** — assinatura assimétrica, cert público distribuído ao app pagador

## Decisão

**HMAC-SHA256** na versão atual (v0.1.0) **com plano explícito de migração** pra
ECDSA + ICP-Brasil em v0.2.0.

## Por quê

### Por que HMAC agora

- **Simplicidade pedagógica** — dá pra entender o fluxo sem se enrolar em PKI
- **Sem dependência externa** — nem cert real nem AC, roda em qualquer ambiente dev
- **Suficiente pra modelar resiliência, observabilidade, hexagonal** — que são o foco do reference

### Por que ECDSA é o caminho de produção

- **Não-repúdio** — recebedor não pode negar que emitiu o payload (HMAC permite)
- **Distribuição de chave** — app pagador valida com **cert público** sem precisar
  ter acesso a chave secreta da maquininha
- **Aderência a ICP-Brasil** — toda infra regulatória brasileira já usa essa cadeia

## Considerações de segurança da implementação atual

- **Constant-time compare** com `MessageDigest.isEqual` na verificação — evita timing attack
- **Chave HMAC mínima 32 bytes em produção** (forçada via lint no `application.yml`)
- **Rotação manual** documentada — `pixnfc.crypto.hmac-key` pode ser trocada com restart

## Migration plan v0.2.0

1. Substituir `HmacPayloadCodec` por `EcdsaIcpBrasilPayloadCodec`
2. Adicionar `pixnfc.crypto.signing-bundle` apontando pro Spring SSL Bundle
   (mesma cadeia ICP-Brasil já usada pro mTLS)
3. Manter `BR1` como prefixo de versão; novo formato é `BR2`
4. Suportar ambos durante transição (codec dispatch por versão)

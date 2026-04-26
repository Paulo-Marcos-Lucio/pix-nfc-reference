# Política de Segurança

## Versões suportadas

Como este é um projeto de referência sob desenvolvimento ativo, **apenas a `main` recebe correções de segurança**. Releases anteriores são marcadas como obsoletas.

| Versão | Suportada |
|---|---|
| `main` (HEAD) | ✅ |
| `v0.x.x` | ⚠️ apenas via novas releases |

## Reportando uma vulnerabilidade

**Não abra issue pública para vulnerabilidades.** Use o canal privado de Security Advisories do GitHub:

👉 https://github.com/Paulo-Marcos-Lucio/pix-nfc-reference/security/advisories/new

Ao reportar, inclua:

- Descrição do problema e impacto potencial
- Passos para reproduzir (PoC se possível)
- Versão / commit afetado
- Sugestão de correção, se houver

### O que esperar

| Etapa | SLA alvo |
|---|---|
| Confirmação de recebimento | até 3 dias úteis |
| Avaliação inicial e severidade | até 7 dias úteis |
| Patch e advisory público | de acordo com a severidade |

Severidade segue [CVSS 3.1](https://www.first.org/cvss/calculator/3.1).

## Escopo

Vulnerabilidades de interesse:

- Bypass de mTLS / aceitação indevida de certificado fora da cadeia ICP-Brasil
- Vazamento de PII em logs (CPF, CNPJ, e-mail, telefone, EVP em claro)
- Cache de chave Pix com TTL maior do que o permitido pelo manual do DICT
- Race conditions em fluxos de claim (portabilidade / reivindicação de posse)
- Injeção (XML / HTTP header / log)
- Deserialização insegura
- Bypass de autenticação / autorização no facade demo
- Configurações inseguras de Spring Security
- Dependências vulneráveis com exploit conhecido

Fora de escopo (mas reporte mesmo assim):

- Engenharia social / phishing
- Ataques de negação de serviço (DoS) sem amplificação
- Vulnerabilidades em dependências sem caminho exploitável demonstrado
- Comportamento do simulador local (não é destinado a produção)

## Disclosure

Trabalhamos em modelo de **disclosure coordenado**: após o patch, o advisory é publicado com crédito ao reporter (a menos que prefira anonimato).

## Hardening default

Esta implementação já aplica:

- mTLS obrigatório em produção via `SslBundle` referenciado em `application.yml`
- Truststore aceita apenas a cadeia ICP-Brasil (validador customizado opcional)
- Logs estruturados sem PII — chaves Pix mascaradas (`02***99`)
- TTL de cache enforced pelo `CacheTtlPolicy` (não configurável acima do limite BCB)
- Rate limiting client-side via Resilience4j para evitar throttling do DICT
- Headers de segurança via Spring Security
- Dependency scanning contínuo (Dependabot + CodeQL + Trivy + Semgrep)
- Validação de payload via Jakarta Bean Validation no facade

## Reconhecimentos

Lista de pesquisadores que reportaram vulnerabilidades válidas será mantida em `SECURITY-HALL-OF-FAME.md` (ainda vazio).

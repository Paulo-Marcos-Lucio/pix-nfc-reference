# ADR 0002 — Payload format `BR1`

**Status:** Accepted · 2026-04-26

## Contexto

Pix por aproximação requer um payload binário curto que possa ser transmitido
via NFC entre maquininha e celular do pagador. A spec oficial do BCB padroniza
o conteúdo (chave, valor, ID, assinatura), mas adota encoding TLV/BER em alguns
documentos e JSON em outros — e ainda está evoluindo.

Este reference precisa de **um formato estável e fácil de decodificar** pra
demonstrar o fluxo end-to-end sem se atar a uma versão específica do manual
que ainda pode mudar.

## Decisão

Usamos um formato textual **`BR1`** com campos delimitados por `|`,
codificado em base64 pra transmissão:

```
BR1|{chargeId}|{merchantIspb}|{keyType}:{keyValue}|{amountCents}|{terminalId}|{label}|{issuedAt}|{validitySec}|{signature}
```

Vantagens:

- **Trivial de decodificar** — split por `|`, sem dependência externa
- **Estável** — não muda quando o manual BCB ajustar TLV
- **Inspeccionável** — base64 vira texto humano-legível com `base64 -d`
- **Compacto o suficiente** — em torno de 200–300 bytes, dentro do limite NFC HCE

## Consequências

- Reference **não é drop-in pra produção real** — adopters precisam trocar o
  `PayloadCodec` pelo formato oficial (TLV BCB ou JSON BCB) quando integrarem
- A interface `PayloadCodec` é o ponto de troca — único arquivo a editar
- Testes de codec ficam simples (string-based, fáceis de raciocinar)

## Roadmap

v0.3.0 substituirá `BR1` por implementação aderente ao manual BCB vigente,
mantendo a interface `PayloadCodec` intacta.

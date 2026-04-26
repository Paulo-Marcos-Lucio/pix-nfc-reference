package dev.pmlsp.pixnfc.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload assinado emitido por uma maquininha (PSP recebedor) e lido via NFC/HCE
 * pelo app do PSP pagador. Estrutura imutável validada criptograficamente.
 *
 * <p>O payload carrega os dados mínimos necessários pra uma transação Pix
 * iniciar — {@link PixKey} do recebedor, valor a cobrar, e identificador único
 * da cobrança. A assinatura HMAC garante que apenas a maquininha autorizada
 * gerou o payload e que ele não foi adulterado em trânsito.
 */
@Value
@Builder
public class NfcPayload {

    /** Identificador da cobrança (UUID v4). Único por maquininha+momento. */
    UUID chargeId;

    /** ISPB do PSP recebedor — ajuda o pagador a validar o payload localmente. */
    Ispb merchantIspb;

    /** Chave Pix do recebedor (mascarada em logs via {@link PixKey#masked()}). */
    PixKey merchantKey;

    /** Valor exato em centavos a cobrar. NFC payloads não permitem valor variável. */
    long amountCents;

    /** Identificador interno da maquininha (terminal ID). */
    String terminalId;

    /** Texto curto exibido ao pagador (ex: "Padaria São José · #4210"). */
    String displayLabel;

    /** Momento em que o payload foi gerado pela maquininha. */
    Instant issuedAt;

    /** Janela em segundos durante a qual o payload é válido (típico: 60–120s). */
    int validitySeconds;

    /** Assinatura HMAC-SHA256 sobre os campos canônicos. */
    String signature;

    /**
     * Retorna {@code true} se o payload já passou da janela de validade.
     * Use {@link java.time.Clock} no caller pra testes determinísticos.
     */
    public boolean isExpired(Instant now) {
        return now.isAfter(issuedAt.plusSeconds(validitySeconds));
    }
}

package dev.pmlsp.pixnfc.domain.model;

/**
 * Razões estruturadas pra recusas/falhas de operações Pix originadas em NFC.
 */
public enum Reason {
    POLICY_VIOLATION,
    INSUFFICIENT_FUNDS,
    INVALID_PAYLOAD,
    EXPIRED,
    DUPLICATE,
    UNAVAILABLE,
    UNKNOWN
}

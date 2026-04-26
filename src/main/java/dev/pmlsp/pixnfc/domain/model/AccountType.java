package dev.pmlsp.pixnfc.domain.model;

/**
 * ISO 20022 account type codes used by the BCB DICT contract.
 *
 * <ul>
 *   <li>{@link #CACC} — Cash account / conta corrente</li>
 *   <li>{@link #SVGS} — Savings / poupança</li>
 *   <li>{@link #TRAN} — Transactional payments / conta de pagamento</li>
 *   <li>{@link #SLRY} — Salary / conta-salário</li>
 * </ul>
 */
public enum AccountType {
    CACC,
    SVGS,
    TRAN,
    SLRY
}

package dev.pmlsp.pixnfc.domain.port.out;

/**
 * Output port for compliance-grade audit logging of DICT operations.
 * Implementations must persist or stream the event somewhere durable enough
 * to satisfy the operational/regulatory retention required by the participant.
 */
public interface AuditLog {

    void record(AuditEvent event);
}

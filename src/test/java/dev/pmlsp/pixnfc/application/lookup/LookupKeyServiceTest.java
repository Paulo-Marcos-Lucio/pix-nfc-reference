package dev.pmlsp.pixnfc.application.lookup;

import dev.pmlsp.pixnfc.domain.exception.KeyNotFoundException;
import dev.pmlsp.pixnfc.domain.model.Account;
import dev.pmlsp.pixnfc.domain.model.AccountType;
import dev.pmlsp.pixnfc.domain.model.DictEntry;
import dev.pmlsp.pixnfc.domain.model.Ispb;
import dev.pmlsp.pixnfc.domain.model.Owner;
import dev.pmlsp.pixnfc.domain.model.PixKey;
import dev.pmlsp.pixnfc.domain.model.PixKeyType;
import dev.pmlsp.pixnfc.domain.policy.CacheTtlPolicy;
import dev.pmlsp.pixnfc.domain.port.in.LookupKeyUseCase;
import dev.pmlsp.pixnfc.domain.port.out.AuditEvent;
import dev.pmlsp.pixnfc.domain.port.out.AuditLog;
import dev.pmlsp.pixnfc.domain.port.out.DictEntryCache;
import dev.pmlsp.pixnfc.domain.port.out.DictGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LookupKeyServiceTest {

    private static final PixKey KEY = PixKey.of(PixKeyType.EMAIL, "loja@merchant.com");
    private static final Ispb PAYER = Ispb.of("12345678");
    private static final Account ACC = new Account(Ispb.of("12345678"), "0001", "111", AccountType.CACC);
    private static final Owner OWNER = Owner.naturalPerson("12345678901", "Alice");
    private static final DictEntry ENTRY = DictEntry.builder()
            .key(KEY).account(ACC).owner(OWNER)
            .creationDate(Instant.now()).keyOwnershipDate(Instant.now())
            .build();

    @Mock DictGateway gateway;
    @Mock DictEntryCache cache;
    @Mock CacheTtlPolicy ttlPolicy;
    @Mock AuditLog audit;

    @InjectMocks LookupKeyService service;

    @Test
    void returnsFromCacheWhenHit() {
        when(cache.get(KEY)).thenReturn(Optional.of(ENTRY));

        DictEntry result = service.lookup(new LookupKeyUseCase.LookupQuery(KEY, PAYER));

        assertThat(result).isEqualTo(ENTRY);
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(captor.capture());
        assertThat(captor.getValue().outcome()).isEqualTo(AuditEvent.Outcome.CACHE_HIT);
        verifyNoInteractions(gateway);
    }

    @Test
    void fetchesFromGatewayWhenCacheMissAndCachesResult() {
        when(cache.get(KEY)).thenReturn(Optional.empty());
        when(gateway.lookup(KEY, PAYER)).thenReturn(Optional.of(ENTRY));
        when(ttlPolicy.ttlFor(ENTRY)).thenReturn(Duration.ofSeconds(60));

        DictEntry result = service.lookup(new LookupKeyUseCase.LookupQuery(KEY, PAYER));

        assertThat(result).isEqualTo(ENTRY);
        verify(cache).put(eq(KEY), eq(ENTRY), eq(Duration.ofSeconds(60)));
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(captor.capture());
        assertThat(captor.getValue().outcome()).isEqualTo(AuditEvent.Outcome.SUCCESS);
    }

    @Test
    void doesNotCacheEntryWhenPolicyReturnsZero() {
        when(cache.get(KEY)).thenReturn(Optional.empty());
        when(gateway.lookup(KEY, PAYER)).thenReturn(Optional.of(ENTRY));
        when(ttlPolicy.ttlFor(ENTRY)).thenReturn(CacheTtlPolicy.ZERO);

        DictEntry result = service.lookup(new LookupKeyUseCase.LookupQuery(KEY, PAYER));

        assertThat(result).isEqualTo(ENTRY);
        verify(cache, never()).put(any(), any(), any());
    }

    @Test
    void throwsKeyNotFoundWhenGatewayReturnsEmpty() {
        when(cache.get(KEY)).thenReturn(Optional.empty());
        when(gateway.lookup(KEY, PAYER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookup(new LookupKeyUseCase.LookupQuery(KEY, PAYER)))
                .isInstanceOf(KeyNotFoundException.class);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(captor.capture());
        assertThat(captor.getValue().outcome()).isEqualTo(AuditEvent.Outcome.NOT_FOUND);
    }
}

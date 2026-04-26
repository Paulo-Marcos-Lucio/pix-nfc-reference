package dev.pmlsp.pixnfc.infrastructure.simulator;

import dev.pmlsp.pixnfc.infrastructure.http.dto.HttpDtos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store in-memory que sustenta o {@link DictSimulatorController}.
 * Pre-populado com uma entrada por tipo de chave para que o simulator funcione
 * out-of-the-box com os exemplos de {@code requests.http}.
 */
public class InMemoryDictStore {

    private final Map<KeyId, HttpDtos.EntryPayload> entries = new ConcurrentHashMap<>();

    public InMemoryDictStore() {
        seed();
    }

    public Optional<HttpDtos.EntryPayload> getEntry(String type, String value) {
        return Optional.ofNullable(entries.get(new KeyId(type, value)));
    }

    public void putEntry(HttpDtos.EntryPayload entry) {
        KeyId id = new KeyId(entry.key().type(), entry.key().value());
        entries.put(id, entry);
    }

    private void seed() {
        Instant now = Instant.now();

        HttpDtos.AccountPayload merchantAcc = new HttpDtos.AccountPayload(
                "12345678", "0001", "0000111111", "CACC");
        HttpDtos.OwnerPayload merchantOwner = new HttpDtos.OwnerPayload(
                "LEGAL_PERSON", "12345678000199", "Padaria São José LTDA", "Padaria São José");
        HttpDtos.OwnerPayload person = new HttpDtos.OwnerPayload(
                "NATURAL_PERSON", "12345678901", "Alice Silva", null);

        List<HttpDtos.EntryPayload> seeds = new ArrayList<>();
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("CNPJ", "12345678000199"),
                merchantAcc, merchantOwner, now, now));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("EMAIL", "loja@padariasaojose.com"),
                merchantAcc, merchantOwner, now, now));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("PHONE", "+5511987654321"),
                merchantAcc, merchantOwner, now, now));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("CPF", "12345678901"),
                merchantAcc, person, now, now));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("EVP", "550e8400-e29b-41d4-a716-446655440000"),
                merchantAcc, person, now, now));

        seeds.forEach(this::putEntry);
    }

    private record KeyId(String type, String value) {}
}

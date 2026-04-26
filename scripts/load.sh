#!/usr/bin/env bash
# Tráfego sintético end-to-end: emite cobranças NFC via /v1/nfc/charges,
# valida e paga via /v1/nfc/payments. Mistura sucessos com unknowns pra
# alimentar os dashboards de overview e resilience.
#
#   make load
#   HOST=http://10.0.0.5:8081 ./scripts/load.sh
#
# Pré-requisitos: app rodando com profile simulator (`make run-sim`).

set -u

HOST="${HOST:-http://localhost:8081}"
DELAY_MS="${DELAY_MS:-100}"

curl_post() {
    local path=$1
    local body=$2
    curl -s -o /dev/null -w "%{http_code}\n" \
        -H "Content-Type: application/json" \
        -X POST "$HOST$path" -d "$body" || true
}

issue_charge() {
    local terminal=$1
    local cents=$2
    curl -s -H "Content-Type: application/json" \
        -X POST "$HOST/v1/nfc/charges" \
        -d "{\"terminalId\":\"$terminal\",\"amountCents\":$cents,\"displayLabel\":\"Loja Demo · #$RANDOM\",\"validitySeconds\":120}" \
        | grep -o '"payloadWire":"[^"]*"' | sed 's/"payloadWire":"//;s/"$//'
}

pay() {
    local wire=$1
    curl_post "/v1/nfc/payments" \
        "{\"payloadWire\":\"$wire\",\"payerAccount\":{\"ispb\":\"99988877\",\"branch\":\"0001\",\"number\":\"1234567\",\"type\":\"CACC\"},\"payerDeviceId\":\"device-load-${RANDOM}\"}"
}

trap 'echo; echo "stopping load"; exit 0' INT TERM

echo "loading $HOST (delay=${DELAY_MS}ms). Ctrl+C to stop."
i=0
while true; do
    AMOUNTS=(150 250 500 1000 2500)
    cents=${AMOUNTS[$((i % 5))]}
    wire=$(issue_charge "T-LOAD-$((i % 4))" "$cents")
    if [ -n "$wire" ]; then
        pay "$wire" >/dev/null
    fi
    sleep "0.$((DELAY_MS / 100))"
    i=$((i + 1))
done

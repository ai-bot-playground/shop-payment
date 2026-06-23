package com.shop.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.payment.domain.OutboxEvent;
import com.shop.payment.domain.Payment;
import com.shop.payment.repo.OutboxRepository;
import com.shop.payment.repo.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock PSP. Decides the outcome by a configurable failure rate, plus a
 * deterministic hook (amount ending in .66 is always declined) used by
 * acceptance tests. Idempotent per orderId: a payment is never charged twice.
 */
@Service
public class PaymentService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PaymentRepository payments;
    private final OutboxRepository outbox;
    private final long latencyMs;
    private volatile double failureRate;

    public PaymentService(PaymentRepository payments,
                          OutboxRepository outbox,
                          @Value("${shop.payment.failure-rate:0.15}") double failureRate,
                          @Value("${shop.payment.latency-ms:200}") long latencyMs) {
        this.payments = payments;
        this.outbox = outbox;
        this.failureRate = failureRate;
        this.latencyMs = latencyMs;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }

    @Transactional
    public void handle(String json) {
        JsonNode e = parse(json);
        if (!"PaymentRequested".equals(e.path("type").asText())) {
            return; // ignore our own results on the shared payment-events topic
        }
        String orderId = e.path("orderId").asText();
        BigDecimal amount = e.path("amount").decimalValue();

        Payment existing = payments.findById(orderId).orElse(null);
        String status;
        if (existing != null) {
            status = existing.getStatus(); // already charged -> reuse, never charge twice
        } else {
            sleepLatency();
            status = decline(amount) ? "FAILED" : "COMPLETED";
            payments.save(new Payment(orderId, status, amount));
        }
        emit("COMPLETED".equals(status) ? "PaymentCompleted" : "PaymentFailed", orderId);
    }

    private boolean decline(BigDecimal amount) {
        long cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (cents % 100 == 66) {
            return true; // deterministic decline hook for acceptance tests
        }
        return ThreadLocalRandom.current().nextDouble() < failureRate;
    }

    private void emit(String type, String orderId) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("type", type);
        body.put("orderId", orderId);
        outbox.save(OutboxEvent.create(eventId, type, orderId, toJson(body)));
    }

    private void sleepLatency() {
        if (latencyMs > 0) {
            try {
                Thread.sleep(latencyMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid event JSON", ex);
        }
    }

    private String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}

package com.bankcore360.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard immutable event envelope used across all BankCore360 Kafka topics.
 *
 * @param eventId     Unique event identifier for idempotency and tracing
 * @param eventType   Domain event classification type
 * @param aggregateId Unique aggregate entity identifier (e.g. application reference or document ID)
 * @param occurredAt  Precise UTC timestamp when the event occurred
 * @param version     Event schema version
 * @param payload     Type-safe event payload containing non-sensitive domain data
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String aggregateId,
        Instant occurredAt,
        int version,
        T payload
) {
    public static <T> EventEnvelope<T> create(String eventType, String aggregateId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID().toString(),
                eventType,
                aggregateId,
                Instant.now(),
                1,
                payload
        );
    }
}


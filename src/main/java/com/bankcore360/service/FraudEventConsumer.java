package com.bankcore360.service;

import com.bankcore360.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FraudEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudEventConsumer.class);

    // Thread-safe set maintaining processed Kafka message keys to guarantee idempotent processing
    private final Set<String> processedMessageKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Idempotent consumer for fraud alert events.
     * Prevents duplicate downstream actions if Kafka rebalances or redelivers messages.
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_FRAUD_ALERT, groupId = "bankcore360-core-group")
    public void consumeFraudAlert(ConsumerRecord<String, String> record) {
        String messageKey = record.key() != null ? record.key() : record.topic() + "-" + record.partition() + "-" + record.offset();

        // Idempotency check
        if (processedMessageKeys.contains(messageKey)) {
            log.warn("⚠️ Duplicate Kafka message detected [key={}]. Ignored idempotently.", messageKey);
            return;
        }

        try {
            log.info("📥 [KAFKA CONSUMER] Processing fraud alert event: key={}, partition={}, offset={}",
                    messageKey, record.partition(), record.offset());

            // Process business logic / downstream analytics
            processedMessageKeys.add(messageKey);
        } catch (Exception e) {
            log.error("Failed processing fraud alert message [{}]: {}", messageKey, e.getMessage());
            throw e; // Triggers DefaultErrorHandler and DeadLetterPublishingRecoverer after retries
        }
    }

    /**
     * Dead Letter Queue (DLQ) listener for handling failed poison pills or corrupted events.
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_FRAUD_ALERT_DLT, groupId = "bankcore360-dlq-group")
    public void consumeDeadLetterAlert(ConsumerRecord<String, String> record) {
        log.error("💀 [DEAD LETTER QUEUE - DLT] Poison message routed to DLQ! Key: {}, Offset: {}, Payload: {}",
                record.key(), record.offset(), record.value());
    }

    public boolean isMessageProcessed(String key) {
        return processedMessageKeys.contains(key);
    }
}


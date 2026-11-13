package com.bankcore360.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public static final String TOPIC_TXN_INITIATED = "bankcore360.transactions.initiated";
    public static final String TOPIC_TXN_APPROVED = "bankcore360.transactions.approved";
    public static final String TOPIC_TXN_FLAGGED = "bankcore360.transactions.flagged";
    public static final String TOPIC_FRAUD_ALERT = "bankcore360.fraud.alerts";

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Strongest durability guarantee
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000); // Fail-fast so outbox handles retries
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic topicTxnInitiated() {
        return TopicBuilder.name(TOPIC_TXN_INITIATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicTxnApproved() {
        return TopicBuilder.name(TOPIC_TXN_APPROVED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicTxnFlagged() {
        return TopicBuilder.name(TOPIC_TXN_FLAGGED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicFraudAlert() {
        return TopicBuilder.name(TOPIC_FRAUD_ALERT).partitions(3).replicas(1).build();
    }
}

package com.bankcore360.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public static final String TOPIC_TXN_INITIATED = "bankcore360.transactions.initiated";
    public static final String TOPIC_TXN_APPROVED = "bankcore360.transactions.approved";
    public static final String TOPIC_TXN_FLAGGED = "bankcore360.transactions.flagged";
    public static final String TOPIC_FRAUD_ALERT = "bankcore360.fraud.alerts";
    public static final String TOPIC_FRAUD_ALERT_DLT = "bankcore360.fraud.alerts.DLT";

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
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "bankcore360-core-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Configures resilient consumer listener container with Dead Letter Queue (DLQ).
     * If message processing fails after 3 retries, DeadLetterPublishingRecoverer dispatches
     * the message to <originalTopic>.DLT for audit and recovery.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Dead Letter Queue recoverer sending failed messages to topic.DLT with exponential backoff
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
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

    @Bean
    public NewTopic topicFraudAlertDlt() {
        return TopicBuilder.name(TOPIC_FRAUD_ALERT_DLT).partitions(3).replicas(1).build();
    }
}

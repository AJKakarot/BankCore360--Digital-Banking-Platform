package com.bankcore360.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Declares Kafka topics and Dead Letter Topics (DLT) for BankCore360.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    @Value("${app.kafka.topics.kyc-events:kyc-events}")
    private String kycEventsTopic;

    @Value("${app.kafka.topics.approval-events:approval-events}")
    private String approvalEventsTopic;

    @Value("${app.kafka.topics.repayment-events:repayment-events}")
    private String repaymentEventsTopic;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setAutoCreate(false);
        return admin;
    }

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(loanEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic loanEventsDltTopic() {
        return TopicBuilder.name(loanEventsTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic kycEventsTopic() {
        return TopicBuilder.name(kycEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic kycEventsDltTopic() {
        return TopicBuilder.name(kycEventsTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic approvalEventsTopic() {
        return TopicBuilder.name(approvalEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic approvalEventsDltTopic() {
        return TopicBuilder.name(approvalEventsTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic repaymentEventsTopic() {
        return TopicBuilder.name(repaymentEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic repaymentEventsDltTopic() {
        return TopicBuilder.name(repaymentEventsTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(notificationEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsDltTopic() {
        return TopicBuilder.name(notificationEventsTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}


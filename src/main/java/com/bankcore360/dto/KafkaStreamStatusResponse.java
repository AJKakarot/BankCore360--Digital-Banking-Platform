package com.bankcore360.dto;

import com.bankcore360.outbox.OutboxEvent;
import com.bankcore360.outbox.ProcessedEvent;

import java.util.List;
import java.util.Map;

public class KafkaStreamStatusResponse {

    private boolean kafkaEnabled;
    private String bootstrapServers;
    private List<TopicDetail> topics;
    private Map<String, Long> outboxStats;
    private List<OutboxEvent> outboxEvents;
    private List<ProcessedEvent> processedEvents;

    public KafkaStreamStatusResponse() {
    }

    public KafkaStreamStatusResponse(
            boolean kafkaEnabled,
            String bootstrapServers,
            List<TopicDetail> topics,
            Map<String, Long> outboxStats,
            List<OutboxEvent> outboxEvents,
            List<ProcessedEvent> processedEvents
    ) {
        this.kafkaEnabled = kafkaEnabled;
        this.bootstrapServers = bootstrapServers;
        this.topics = topics;
        this.outboxStats = outboxStats;
        this.outboxEvents = outboxEvents;
        this.processedEvents = processedEvents;
    }

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public void setKafkaEnabled(boolean kafkaEnabled) {
        this.kafkaEnabled = kafkaEnabled;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public List<TopicDetail> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDetail> topics) {
        this.topics = topics;
    }

    public Map<String, Long> getOutboxStats() {
        return outboxStats;
    }

    public void setOutboxStats(Map<String, Long> outboxStats) {
        this.outboxStats = outboxStats;
    }

    public List<OutboxEvent> getOutboxEvents() {
        return outboxEvents;
    }

    public void setOutboxEvents(List<OutboxEvent> outboxEvents) {
        this.outboxEvents = outboxEvents;
    }

    public List<ProcessedEvent> getProcessedEvents() {
        return processedEvents;
    }

    public void setProcessedEvents(List<ProcessedEvent> processedEvents) {
        this.processedEvents = processedEvents;
    }

    public static class TopicDetail {
        private String name;
        private String consumerGroup;
        private String description;

        public TopicDetail() {
        }

        public TopicDetail(String name, String consumerGroup, String description) {
            this.name = name;
            this.consumerGroup = consumerGroup;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}


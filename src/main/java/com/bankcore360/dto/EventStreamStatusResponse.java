package com.bankcore360.dto;

import com.bankcore360.outbox.OutboxEvent;
import com.bankcore360.outbox.ProcessedEvent;

import java.util.List;
import java.util.Map;

public class EventStreamStatusResponse {

    private boolean streamEnabled;
    private String streamEngine;
    private List<ChannelDetail> channels;
    private Map<String, Long> outboxStats;
    private List<OutboxEvent> outboxEvents;
    private List<ProcessedEvent> processedEvents;

    public EventStreamStatusResponse() {
    }

    public EventStreamStatusResponse(
            boolean streamEnabled,
            String streamEngine,
            List<ChannelDetail> channels,
            Map<String, Long> outboxStats,
            List<OutboxEvent> outboxEvents,
            List<ProcessedEvent> processedEvents
    ) {
        this.streamEnabled = streamEnabled;
        this.streamEngine = streamEngine;
        this.channels = channels;
        this.outboxStats = outboxStats;
        this.outboxEvents = outboxEvents;
        this.processedEvents = processedEvents;
    }

    public boolean isStreamEnabled() {
        return streamEnabled;
    }

    public void setStreamEnabled(boolean streamEnabled) {
        this.streamEnabled = streamEnabled;
    }

    public String getStreamEngine() {
        return streamEngine;
    }

    public void setStreamEngine(String streamEngine) {
        this.streamEngine = streamEngine;
    }

    public List<ChannelDetail> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelDetail> channels) {
        this.channels = channels;
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

    public static class ChannelDetail {
        private String name;
        private String consumerGroup;
        private String description;

        public ChannelDetail() {
        }

        public ChannelDetail(String name, String consumerGroup, String description) {
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

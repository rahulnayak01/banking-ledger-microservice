package com.example.ledger.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${ledger.kafka.topics.transaction-events:transaction-events}")
    private String transactionEventsTopic;

    @Value("${ledger.kafka.topics.balance-events:balance-events}")
    private String balanceEventsTopic;

    @Value("${ledger.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    @Value("${ledger.kafka.topics.transaction-events-dlt:transaction-events-dlt}")
    private String transactionEventsDltTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(transactionEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic balanceEventsTopic() {
        return TopicBuilder.name(balanceEventsTopic)
                .partitions(3)
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
    public NewTopic transactionEventsDltTopic() {
        return TopicBuilder.name(transactionEventsDltTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

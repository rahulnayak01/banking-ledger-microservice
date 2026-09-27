package com.example.ledger.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * DeadLetterConsumer — monitors the dead-letter topic (DLQ / DLT) for messages
 * that exhausted all retry attempts in main consumer groups.
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @KafkaListener(
            topics = "${ledger.kafka.topics.transaction-events-dlt:transaction-events-dlt}",
            groupId = "dlt-monitoring-group"
    )
    public void consumeDlt(ConsumerRecord<String, String> record) {
        log.error("[DLT MONITOR] Poison pill / unprocessable message received on DLT! Key: {}, Partition: {}, Offset: {}, Value: {}",
                record.key(), record.partition(), record.offset(), record.value());
    }
}

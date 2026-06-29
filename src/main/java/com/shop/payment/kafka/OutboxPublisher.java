package com.shop.payment.kafka;

import com.shop.payment.domain.OutboxEvent;
import com.shop.payment.repo.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "shop.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public OutboxPublisher(OutboxRepository outbox,
                           KafkaTemplate<String, String> kafka,
                           @Value("${shop.payment.payment-events-topic:payment-events}") String topic) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${shop.outbox.poll-ms:1000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> batch = outbox.findTop100ByPublishedAtIsNullOrderByIdAsc();
        for (OutboxEvent e : batch) {
            kafka.send(topic, e.getMsgKey(), e.getPayload());
            e.markPublished();
        }
    }
}

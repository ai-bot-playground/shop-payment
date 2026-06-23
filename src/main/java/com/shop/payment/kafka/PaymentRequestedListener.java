package com.shop.payment.kafka;

import com.shop.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestedListener {

    private final PaymentService service;

    public PaymentRequestedListener(PaymentService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${shop.payment.payment-events-topic:payment-events}")
    public void onMessage(String value) {
        service.handle(value);
    }
}

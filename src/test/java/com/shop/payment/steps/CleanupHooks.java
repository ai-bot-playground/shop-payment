package com.shop.payment.steps;

import com.shop.payment.repo.OutboxRepository;
import com.shop.payment.repo.PaymentRepository;
import io.cucumber.java.Before;

public class CleanupHooks {

    private final PaymentRepository payments;
    private final OutboxRepository outbox;

    public CleanupHooks(PaymentRepository payments, OutboxRepository outbox) {
        this.payments = payments;
        this.outbox = outbox;
    }

    @Before
    public void clean() {
        outbox.deleteAll();
        payments.deleteAll();
    }
}

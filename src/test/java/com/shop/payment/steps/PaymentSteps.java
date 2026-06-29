package com.shop.payment.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.payment.repo.OutboxRepository;
import com.shop.payment.repo.PaymentRepository;
import com.shop.payment.service.PaymentService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentSteps {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PaymentService service;
    private final PaymentRepository payments;
    private final OutboxRepository outbox;

    public PaymentSteps(PaymentService service, PaymentRepository payments, OutboxRepository outbox) {
        this.service = service;
        this.payments = payments;
        this.outbox = outbox;
    }

    @Given("the payment failure rate is {double}")
    public void theFailureRateIs(double rate) {
        service.setFailureRate(rate);
    }

    @When("a PaymentRequested event arrives for order {string} amount {double}")
    public void paymentRequested(String orderId, double amount) {
        service.handle(paymentRequestedJson(orderId, amount));
    }

    @Given("a PaymentRequested event for order {string} was already processed with result {word}")
    public void alreadyProcessed(String orderId, String result) {
        service.setFailureRate("COMPLETED".equals(result) ? 0.0 : 1.0);
        service.handle(paymentRequestedJson(orderId, 10.00));
    }

    @When("the PaymentRequested event for order {string} is delivered again")
    public void deliveredAgain(String orderId) {
        service.handle(paymentRequestedJson(orderId, 10.00));
    }

    @Then("a PaymentCompleted event is published for {string}")
    public void paymentCompletedPublished(String orderId) {
        assertOutbox("PaymentCompleted", orderId);
    }

    @Then("a PaymentFailed event is published for {string}")
    public void paymentFailedPublished(String orderId) {
        assertOutbox("PaymentFailed", orderId);
    }

    @Then("the previous result {word} is reused")
    public void previousResultReused(String result) {
        assertOutbox("COMPLETED".equals(result) ? "PaymentCompleted" : "PaymentFailed", "O1");
    }

    @Then("the customer is charged only once")
    public void chargedOnlyOnce() {
        assertThat(payments.count()).isEqualTo(1L);
    }

    private void assertOutbox(String type, String orderId) {
        boolean found = outbox.findAll().stream()
                .anyMatch(o -> o.getType().equals(type) && payloadOrderId(o.getPayload()).equals(orderId));
        assertThat(found).as("%s for order %s in outbox", type, orderId).isTrue();
    }

    private String payloadOrderId(String payload) {
        try {
            return MAPPER.readTree(payload).path("orderId").asText();
        } catch (Exception e) {
            return "";
        }
    }

    private String paymentRequestedJson(String orderId, double amount) {
        return "{\"eventId\":\"" + UUID.randomUUID() + "\",\"type\":\"PaymentRequested\",\"orderId\":\""
                + orderId + "\",\"amount\":" + amount + "}";
    }
}

Feature: Payment processing (mock)
  A mock PSP with a configurable failure rate, used to exercise the
  compensation path. A payment is never charged twice.

  Scenario: Successful payment
    Given the payment failure rate is 0.0
    When a PaymentRequested event arrives for order "O1" amount 199.99
    Then a PaymentCompleted event is published for "O1"

  Scenario: Declined payment
    Given the payment failure rate is 1.0
    When a PaymentRequested event arrives for order "O2" amount 50.00
    Then a PaymentFailed event is published for "O2"

  Scenario: Deterministic decline hook for acceptance tests
    Given the payment failure rate is 0.0
    When a PaymentRequested event arrives for order "O3" amount 6.66
    Then a PaymentFailed event is published for "O3"

  Scenario: A payment is never charged twice (idempotent)
    Given a PaymentRequested event for order "O1" was already processed with result COMPLETED
    When the PaymentRequested event for order "O1" is delivered again
    Then the previous result COMPLETED is reused
    And the customer is charged only once

package com.shop.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentApplication {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplication.class);

    public static void main(String[] args) {
        log.info("Starting shop-payment service");
        SpringApplication.run(PaymentApplication.class, args);
    }
}

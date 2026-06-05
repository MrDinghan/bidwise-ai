package com.bidwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point for the BidWise AI backend.
 */
@SpringBootApplication
@EnableScheduling
public class BidwiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BidwiseApplication.class, args);
    }
}

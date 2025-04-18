package com.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoundUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoundUpApplication.class, args);
        System.out.println("RoundUp Application is running...");
        System.out.println("Access the round-up processing endpoint at: http://localhost:8080/roundup/process");
    }
}

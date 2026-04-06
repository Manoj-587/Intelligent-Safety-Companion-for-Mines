package com.minesafety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MineSafetyBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MineSafetyBackendApplication.class, args);
    }
}

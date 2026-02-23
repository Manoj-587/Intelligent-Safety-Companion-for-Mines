package com.minesafety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MineSafetyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MineSafetyBackendApplication.class, args);
        System.out.print("Hello");
        System.out.print("World");
    }
}

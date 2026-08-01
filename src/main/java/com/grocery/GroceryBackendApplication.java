package com.grocery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GroceryBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(GroceryBackendApplication.class, args);
    }
}

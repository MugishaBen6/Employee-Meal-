package com.emeal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmployeeMealApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeMealApplication.class, args);
    }
}

package com.backend.nova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NovaApplication {

    public static void main(String[] args) {

        SpringApplication.run(NovaApplication.class, args);

    }

}
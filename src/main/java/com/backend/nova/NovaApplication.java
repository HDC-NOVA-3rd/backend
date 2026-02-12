package com.backend.nova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class NovaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaApplication.class, args);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("super1234! = " + encoder.encode("super1234!"));
        System.out.println("admin1234! = " + encoder.encode("admin1234!"));
    }

}
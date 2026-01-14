package com.bugzero.rarego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RaregoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaregoApplication.class, args);
    }

}

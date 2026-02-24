package com.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TerminalMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TerminalMonitorApplication.class, args);
    }
}

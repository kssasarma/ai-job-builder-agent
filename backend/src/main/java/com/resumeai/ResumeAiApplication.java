package com.resumeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ResumeAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAiApplication.class, args);
    }
}

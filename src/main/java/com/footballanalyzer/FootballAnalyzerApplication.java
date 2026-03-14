package com.footballanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FootballAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballAnalyzerApplication.class, args);
    }
}

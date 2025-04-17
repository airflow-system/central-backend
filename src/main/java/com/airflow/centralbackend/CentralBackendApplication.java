package com.airflow.centralbackend;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CentralBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CentralBackendApplication.class, args);
    }

}

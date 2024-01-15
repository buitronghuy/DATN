package com.example.federatedserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FederatedserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(FederatedserverApplication.class, args);
	}

}

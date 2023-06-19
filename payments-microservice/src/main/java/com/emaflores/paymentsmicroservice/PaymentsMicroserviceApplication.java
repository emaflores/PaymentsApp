package com.emaflores.paymentsmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PaymentsMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentsMicroserviceApplication.class, args);
	}

}

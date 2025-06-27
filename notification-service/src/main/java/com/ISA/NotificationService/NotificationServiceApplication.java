package com.ISA.Restaurant;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

//@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class }
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class RestaurantApplication {


	public static void main(String[] args) {
		// Initialize Dotenv
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing() // Continue if the .env file is missing
				.load();

		// Load environment variables into system properties
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

		SpringApplication.run(RestaurantApplication.class, args);
	}

}

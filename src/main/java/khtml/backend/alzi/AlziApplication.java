package khtml.backend.alzi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlziApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlziApplication.class, args);
	}

}

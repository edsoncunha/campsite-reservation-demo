package io.github.edsoncunha.upgrade.takehome;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info( description = "Campsite Management API", version = "0.1", contact = @Contact(name = "Edson Cunha",
		email = "edsoncamposcunha@gmail.com", url = "https://www.linkedin.com/in/edsoncunha/")))
public class TakehomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TakehomeApplication.class, args);
	}

}

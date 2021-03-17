package gov.dhs.cisa.flare.misp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import gov.dhs.cisa.flare.misp.util.SSLHostnameVerification;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

/*
 * Runs the Spring Boot application
 */

@SpringBootApplication
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
public class Application {

	public static void main(String[] args) {
		//SSL Hostname verification On/Off can be set in applicaiton.properties
		SSLHostnameVerification.executeConfigration();
		SpringApplication.run(Application.class, args);
	}
}

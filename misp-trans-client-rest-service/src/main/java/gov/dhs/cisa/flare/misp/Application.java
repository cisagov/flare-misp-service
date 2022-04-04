package gov.dhs.cisa.flare.misp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import gov.dhs.cisa.flare.misp.util.SSLHostnameVerification;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

/*
 * Runs the Spring Boot application
 */

@SpringBootApplication
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
public class Application {

    private static Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
		//SSL Hostname verification On/Off can be set in applicaiton.properties
		SSLHostnameVerification.executeConfigration();
		SpringApplication.run(Application.class, args);
	}
}

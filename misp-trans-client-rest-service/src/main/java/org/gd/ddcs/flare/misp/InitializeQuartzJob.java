package org.gd.ddcs.flare.misp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * The 'Job' interface basically wants us to to define what code must be executed when it's time to do something.
 */
@DisallowConcurrentExecution
public class InitializeQuartzJob implements Job
{
    private static Logger log = LoggerFactory.getLogger(InitializeQuartzJob.class);
	private String urlStr = Config.getProperty("mtc.baseurl") + "?processType=" + Config.getProperty("mtc.processtype");

	/**
	 * This method defines the 'job' that the Quartz Scheduler will execute
	 *
	 * This job generates an HTTP request (to this same REST service)
	 * It will start a one-time poll via CTI Toolkit, to search for STIX on the TAXI Server,
	 * convert it to MISP event and then upload it to the MISP Server.
	 * (Assuming that ‘mtc.processtype’ config property is set to  the string ‘stixToMisp’)
	 */
	public void execute(JobExecutionContext context) throws RuntimeException {
		log.info("Removed CTI Toolkit call - this is a placeholder");
		/* Original CTI Toolkit
		try {
			URI uri = new URI(urlStr);
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

			if (response.getStatusCode() == HttpStatus.OK) {
				log.info("Received OK status from {}", urlStr);
			} else {
				log.error("Received status code {}", response.getStatusCodeValue());
			}

			log.info("Initialize quartz received response body: {}",response.getBody());

		  } catch (URISyntaxException e) {
			  log.error("Error: Malformed URL: {} {}", urlStr, e.getMessage());
		  }
	   */
	}
}

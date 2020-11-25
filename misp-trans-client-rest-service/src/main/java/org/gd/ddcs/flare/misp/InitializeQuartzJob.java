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
	 * This job generates an HTTP request to this same REST service - to trigger the controller
	 * The controller will poll via the cti-toolkit to get stix files then use the MISP Service to transform the files.
	 */
	public void execute(JobExecutionContext context) throws RuntimeException {
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
	}
}

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

@DisallowConcurrentExecution
public class InitializeQuartzJob implements Job
{
    private static Logger log = LoggerFactory.getLogger(InitializeQuartzJob.class);
	private String urlStr = Config.getProperty("mtc.baseurl") + "?processType=" + Config.getProperty("mtc.processtype");

	public void execute(JobExecutionContext context) throws RuntimeException {
		URI uri = null;
		try {
			uri = new URI(urlStr);
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			
			if (response.getStatusCode() != HttpStatus.OK) {
				log.error("Received status code {}", response.getStatusCodeValue());
			}
			
			log.info("Initialize quartz received response body: {}",response.getBody());

		  } catch (URISyntaxException e) {
			  log.error("Error: URI {} is a malformed URL: {}", uri, e.getMessage());
		  }
	}
}

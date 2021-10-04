package gov.dhs.cisa.flare.misp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

public class Taxii11RequestSubmit {
    private static Logger log = LoggerFactory.getLogger(Taxii11RequestSubmit.class);

    public static ResponseEntity<String> postRequest(RestTemplate restTemplate, String url, HttpEntity<String> entity, Class<String> classType) throws URISyntaxException {
        log.info("Posting content to >>>>>>>>>>> {} .... Waiting for Taxii Server to respond", url);
        URI uri = new URI(url);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);

        log.info("Status code: {}", response.getStatusCode());
        log.debug("Content: {}", response.getBody());
        return response;
    }
}

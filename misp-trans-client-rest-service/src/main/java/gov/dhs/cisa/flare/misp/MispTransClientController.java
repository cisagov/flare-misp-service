package gov.dhs.cisa.flare.misp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This controller pulls events from a FLARE/taxii server and pushes them out to
 * a MISP server
 * <p>
 * Note: This controller does not specify GET vs. PUT, POST, and so forth,
 * because @RequestMapping maps all HTTP operations by default.
 * Use @RequestMapping(method=GET) to narrow this mapping.
 */

@RestController
public class MispTransClientController {

    @Autowired
    TwoWaySslConfiguration twoWaySslConfiguration;

    @Autowired
    Taxii11Response taxii11Response;

    private final AtomicLong counter = new AtomicLong();
    private static final Logger log = LoggerFactory.getLogger(MispTransClientController.class);
    private RestTemplate restTemplate = new RestTemplate();

    /*
     * Sample URLs:
     *
     * https://localhost:8443/misptransclient?processType=xmlOutput
     * https://localhost:8443/misptransclient?processType=stixToMisp
     * https://localhost:8443/misptransclient?processType=xmlOutput&collection=MSIP
     * https://localhost:8443/misptransclient?processType=xmlOutput&collection=
     * NCPS_Automated
     *
     */

    public List<Node> getContentBlocks(Node root) {
        List<Node> contentBlocks = new ArrayList<Node>();
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ("taxii_11:Content_Block".equals(node.getNodeName())) {
                contentBlocks.add(node);
            }
        }
        return contentBlocks;
    }

    public MispTransClient processMispTransClient(String processType, String collectionName) {
        String url = Config.getProperty("stixtransclient.poll.baseurl");

        try {
            log.info("MispTransClientController:processMispTransClient: ==========> processType: {}", processType);

            if ("".equals(collectionName)) {
                collectionName = Config.getProperty("stixtransclient.source.collection");
            }

            log.info("MispTransClientController:processMispTransClient: to {}", url);

            Instant dateStop = Config.getEndTime();
            Instant dateStart = Config.getStartTime();

            String body = Taxii11Request.makeBody(dateStart, dateStop, collectionName, false, "FULL");
            HttpHeaders headers = Taxii11Request.makeHeaders();
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            restTemplate = twoWaySslConfiguration.restTemplate();

            log.debug(">>>>>>>> {}", restTemplate.getClass());

            ResponseEntity<String> response = Taxii11RequestSubmit.postRequest(restTemplate, url, entity, String.class);
            log.debug(">>>>>>>>>>>>>>>>>> response body: \n\n{}", response.getBody());
            log.info(">>>>>>>> {}", response.getStatusCode());

            Document doc = taxii11Response.processPollResponse(response);
            Node pollResponse = taxii11Response.getPollRequest(doc);
            List<Node> contentBlocks = taxii11Response.getContentBlocks(pollResponse);
            List<Node> stixPackages = taxii11Response.getStixPackages(contentBlocks);

            taxii11Response.processPollResponse(stixPackages, processType);
            return new MispTransClient(counter.incrementAndGet(), String.valueOf(response.getStatusCode()), null,
                processType, collectionName, dateStart.toString(), dateStop.toString());
        } catch (Exception e) {
            log.info(">>>>>>>>>>>>> Connection Timeout error occurred : {} ", url);
            log.info(">>>>>>>>>>>>> Please check the URL, Collection Name, and Authorization");
        }

        return null;
    }

    @PostMapping("/misptransclient")
    public MispTransClient event(@RequestParam(value = "processType", defaultValue = "xmlOutput") String processType,
                                 @RequestParam(value = "collection", defaultValue = "") String collection)
        throws ParserConfigurationException, TransformerException, SAXException, IOException, URISyntaxException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        log.info("MispTransClientController:misptransclient:processType:{} collection: {}", processType, collection);

        return processMispTransClient(processType, collection);
    }

    @GetMapping("/checkTaxiiStatus")
    private HealthCheckResponse checkTaxiiStatus() {
        return checkConnection("FLARE/TAXII", "stixtransclient.poll.baseurl");
    }

    @GetMapping("/checkMispStatus")
    private HealthCheckResponse checkMispStatus() {
        return checkConnection("Misp", "stixtransclient.misp.url");
    }

    /**
     * @param resourceType      The name of the server we're connecting to
     * @param configPropertyKey The config.property key which has the URL we're
     *                          checking
     */
    private HealthCheckResponse checkConnection(String resourceType, String configPropertyKey) {
        String url = Config.getProperty(configPropertyKey);
        log.info("Performing {} connection health check against url: {}", resourceType, url);

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.error("\n\nERROR: Config value for {} URL {} is a malformed URL. {}\n\n", configPropertyKey, url,
                e.getMessage());
            return new HealthCheckResponse(resourceType, url, 408);
        }
        ResponseEntity<String> response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("X-TAXII-Content-Type", "urn:taxii.mitre.org:message:xml:1.1");
            headers.set("X-TAXII-Accept", "urn:taxii.mitre.org:message:xml:1.1");
            headers.set("X-TAXII-Services", "urn:taxii.mitre.org:services:1.1");
            headers.set("X-TAXII-Protocol", "urn:taxii.mitre.org:protocol:http:1.0");
            HttpEntity<String> entity = new HttpEntity<>("body", headers);
            response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
        } catch (RestClientException e) {
            String resolution = "";
            if (e.getCause().getMessage().contains("unable to find valid certification path to requested target")) {
                resolution = "Suggested resolution: Add " + uri.getHost()
                    + "'s Intermediate-CA public certificate to the java trust store.\n"
                    + "For example: sudo keytool -importcert -keystore /usr/local/java/jdk/jre/lib/security/cacerts -file /home/user/ca.crt -alias \"flare-ca\"\n"
                    + "\n";
            }

            log.error("\n\nERROR: Health check connection test for " + resourceType + " with " + configPropertyKey + "="
                + url + " was unsuccessful due to: \n" + e.getCause() + "\n\n" + resolution);

            return new HealthCheckResponse(resourceType, url, 0);
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("Successful response from {} Server {}", resourceType, url);
        } else {
            log.error("Unsuccessful response from {} Server {} \nResponse: {}", resourceType, url, response.toString());
        }
        return new HealthCheckResponse(resourceType, url, response.getStatusCodeValue());
    }

    @RequestMapping("/refreshConfig")
    public ResponseEntity<Object> refreshConfig() {
        HashMap<String, String> map = new HashMap<>();
        map.put("Controller", "/refreshConfig:");
        log.info("Controller refreshConfig - Attempting to Reload Configuration Properties and Update the Service.");
        log.info("Controller refreshConfig Step 1of5 - Attempt to STOP Quartz Jobs...");
        map.put("Step 1 of 5", "Attempt to STOP Quartz Jobs...");

        stopQuartzJobs();
        log.info("Controller refreshConfig Step 2of5 - Attempt to STOP Quartz Scheduler...");
        map.put("Step 2 of 5", "Attempt to STOP Quartz Scheduler...");
        stopQuartzScheduler();

        log.info("Controller refreshConfig Step 3of5 - Attempt to reload Configuration Properties...");
        map.put("Step 3 of 5", "Attempt to reload Configuration Properties...");
        log.info(
            "Controller refreshConfig Step 4of5 - Reset Begin/End TimeStamps. Will be initialized via Configuration Properties...");
        map.put("Step 4 of 5", "Reset Begin/End TimeStamps. Will be initialized via Configuration Properties...");
        Config.loadConfig();

        log.info("Controller refreshConfig Step 5of5 - Attempt to ReSTART the Quartz Scheduler...");
        map.put("Step 5 of 5", "Attempt to ReSTART the Quartz Scheduler...");
        initQuartz();

        Map<String, String> sortedMap = new TreeMap<>(map);
        return new ResponseEntity<>(sortedMap, HttpStatus.OK);
    }

    @RequestMapping("/initQuartz")
    public ResponseEntity<String> initQuartz() {
        StringBuilder sbf = new StringBuilder("Controller - Asked to Initialize Quartz Scheduler...\n");

        log.info("Controller - Asked to Initialize Quartz Scheduler...");

        String quartzFrequencyStr = Config.getProperty("mtc.quartz.frequency");
        int quartzFrequency = 2;

        if (quartzFrequencyStr != null && !quartzFrequencyStr.isEmpty()) {
            quartzFrequency = Integer.parseInt(quartzFrequencyStr);
        }

        try {
            JobDetail job1 = JobBuilder.newJob(InitializeQuartzJob.class).withIdentity("initializeQuartzJob", "group1")
                .build();

            Trigger trigger1 = TriggerBuilder.newTrigger().withIdentity("simpleTrigger", "group1")
                .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(quartzFrequency)).build();

            Scheduler scheduler1 = new StdSchedulerFactory().getScheduler();
            if (!scheduler1.isStarted()) {
                sbf.append("Quartz Scheduler has not been started automatically. Or has previously stopped. Starting it now.");
                log.info(
                    "Controller- Quartz Scheduler has not been started automatically. Or has previously stopped. Starting it now.");
                scheduler1.start();
                scheduler1.scheduleJob(job1, trigger1);
            } else {
                sbf.append("uartz Scheduler has already been started.");
                log.warn("Controller- Quartz Scheduler has already been started.");
            }

        } catch (SchedulerException e) {
            sbf.append("Exception when trying to get Quartz Scheduler:").append(e.getMessage());
            log.error("Exception when trying to get Quartz Scheduler: {}", e.getMessage());
        }

        return new ResponseEntity<>(sbf.toString(), HttpStatus.OK);
    }

    @RequestMapping("/listQuartzJobs")
    public ResponseEntity<String> listQuartzJobs() {
        log.info("List Quartz Jobs...");
        StringBuilder quartzJobsString = new StringBuilder("Quartz Jobs:\n");

        try {
            Scheduler scheduler = new StdSchedulerFactory().getScheduler();

            for (String groupName : scheduler.getJobGroupNames()) {

                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                    String jobName = jobKey.getName();
                    String jobGroup = jobKey.getGroup();

                    // get job's trigger
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                    Date nextFireTime = triggers.get(0).getNextFireTime();

                    log.info("[jobName] : " + jobName + " [groupName] : " + jobGroup + " - " + nextFireTime);

                    quartzJobsString.append("jobName: ").append(jobName).append("\ngroupName: ").append(jobGroup)
                        .append(" - ").append(nextFireTime).append("\n");

                }
            }
        } catch (SchedulerException e) {
            quartzJobsString.append("Error:").append(e.getSuppressed());
            log.error("Exception when trying to get Quartz Scheduler: {}", e.getMessage());
        }

        log.info("Quartz Jobs:" + quartzJobsString.toString());

        return new ResponseEntity<>(quartzJobsString.toString(), HttpStatus.OK);

    }

    @RequestMapping("/stopQuartzJobs")
    public ResponseEntity<String> stopQuartzJobs() {

        StringBuilder sbf = new StringBuilder("Stop Quartz Jobs:");
        try {
            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey("simpleTrigger", "group1");
            scheduler.unscheduleJob(triggerKey);
            sbf.append(".....the scheduler stopped the Jobs for group1.");
            log.info("The scheduler stopped the Jobs for group1");
            // redirect(action: "view", params: params)
        } catch (SchedulerException e) {
            sbf.append("Error:").append(e.getMessage());
            log.error("SchedulerException: " + e.getMessage());
        }
        return new ResponseEntity<>(sbf.toString(), HttpStatus.OK);
    }

    // DONT allow stopQuartzScheduler() to be called via the REST API.
    // Only used for internal purposes when reloading configuration properties and
    // must insure scheduler updated as well.
    // The scheduler will be terminated when the jvm is terminated.
    private void stopQuartzScheduler() {
        try {
            log.info("Controller - Attempting to STOP the Quartz Scheduler...");
            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.shutdown();
            log.info("Controller - The scheduler is STOPPED successfully.");
        } catch (SchedulerException e) {
            log.error("Exception when trying to Stop the Quartz Scheduler. " + e.getMessage());
        }
    }


    private boolean checkResources() {
        boolean resourcesAvailable = true;

        HealthCheckResponse response = checkTaxiiStatus();
        if (response.getStatusCode() == 200) {
            log.info("TAXII health check passed.");
        } else {
            log.error("TAXII health check failed.");
            resourcesAvailable = false;
        }

        response = checkMispStatus();
        if (response.getStatusCode() == 200) {
            log.info("Misp health check passed.");
        } else {
            log.error("Misp health check failed.");
            resourcesAvailable = false;
        }

        return resourcesAvailable;
    }

}

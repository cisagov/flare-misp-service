package org.gd.ddcs.flare.misp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.gd.ddcs.flare.misp.util.EncryptionUtil;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/*
 * This controller pulls events from a FLARE/taxii server and pushes them out to a MISP server using the
 * provided stixtransclient.py script
 * 
 * Note:
 * This controller does not specify GET vs. PUT, POST, and so forth, 
 * because @RequestMapping maps all HTTP operations by default. 
 * Use @RequestMapping(method=GET) to narrow this mapping. 
 * 
 * 
 */

@RestController
public class MispTransClientController {

    private final AtomicLong counter = new AtomicLong();
    private static Logger log = LoggerFactory.getLogger(MispTransClientController.class);
    boolean resourcesAvailable = false;
    String beginTimestamp = null;
    String endTimestamp = null;
   
    /*
     * Sample URLs:
     * 
     * http://localhost:8080/misptransclient?processType=xmlOutput
     * http://localhost:8080/misptransclient?processType=stixToMisp
     * http://localhost:8080/misptransclient?processType=xmlOutput&collection=MSIP
     * http://localhost:8080/misptransclient?processType=xmlOutput&collection=NCPS_Automated
     *  
     */

    @RequestMapping("/misptransclient")
    public MispTransClient event(@RequestParam(value="processType", defaultValue="xmlOutput") String processType,
    							 @RequestParam(value="collection", defaultValue="") String collection) {    	
    	
    	//Assume process has failed until evidence of success is discovered
    	String status = "Failed";  
    	String detailedStatus = "failed to completed sucessfully.  Review log for additional details.";
    	
    	//ArrayLists to capture exceptions, and warnings
    	ArrayList<String>errorAL = new ArrayList<String>();
    	ArrayList<String>warningAL = new ArrayList<String>();

        boolean hasError = false;
        boolean hasWarning = false;
    	boolean taxiiServerNotResponding = false;
    	
    	String validatedCollection = "";
    	String validatedProcessType = "";
    	
    	BufferedReader stdError =  null;
    	
    	try {
	    	log.info("Processing events...");
	    	
	    	resourcesAvailable = checkResources();
	    	
	    	if(resourcesAvailable) {
	    		//If collection is not defined, use default value
	    		if("".equals(collection)) {
	    			collection = Config.getProperty("stixtransclient.source.collection"); 
	    		}
	    		
	    		//Validate collection
    			validatedCollection =  this.validateValue(collection,"stixtransclient.source.collection.validvalues");
    			
    			if("".equals(validatedCollection)) {
    				log.error("Invalid Collection:" + collection);

		    		detailedStatus = "Exception: Invalid Collection";
    		    	
    		        return new MispTransClient(counter.incrementAndGet(),
    		        		            status,
    		        		            detailedStatus,
    		                            processType,
    		                            collection,
    		                            this.getBeginTimestamp(),
    		                            this.getEndTimestamp()
    		        					);
    			}

	    		//If processType is not defined, use default value
	    		if("".equals(processType)) {
	    			processType = Config.getProperty("mtc.processtype"); 
	    		}
	    		
	    		validatedProcessType = this.validateValue(processType,"mtc.processtype.validvalues"); 
    			
    			if("".equals(validatedProcessType)) {
    				log.error("Invalid Process Type:" + processType);

		    		detailedStatus = "Exception: Invalid Process Type";
    		    	
    		        return new MispTransClient(counter.incrementAndGet(),
    		        		            status,
    		        		            detailedStatus,
    		                            processType,
    		                            collection,
    		                            this.getBeginTimestamp(),
    		                            this.getEndTimestamp()
    		        					);
    			}

	    		//Construct command
		    	String cmd = getCommandStr(validatedProcessType, validatedCollection);
		    	//log.info("cmd: " + cmd);;
		    	
		    	Process p = Runtime.getRuntime().exec(cmd);
	            //BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	
	            // Read the output from the command
	            String s = null;
	            
	            //while ((s = stdInput.readLine()) != null)
	            //{
	            //	log.info(s);
	            //}
	            
	           // Read any errors from the attempted command
	           while ((s = stdError.readLine()) != null)
	           {
	        	    if (s.equals("Exception: didn't get a poll response")) {
		    			errorAL.add(s);
			    		log.error(s);
			    		taxiiServerNotResponding = true;
		                hasError = true;
	        	    }
		        	else if ( s.indexOf("ERROR") >=0 ||
		        			  s.indexOf("Error") >=0)
	        	    {
		        	    log.info("Error String: " +  s);
		    			errorAL.add(s);
			    		log.error(s);
		                hasError = true;
	        	    }	        	    
	        	    else if (s.indexOf("Exception:") >= 0) {
		    			errorAL.add(s);
			    		log.error(s);
		                hasError = true;
		    		}
		    		else {
		    			if(!suppressWarning(s)) {
		    			  warningAL.add(s);
			    		  log.info(s);
		                  hasWarning = true;
		    			}		                
		    		}
	           }
		    	
		    	p.waitFor();
		    	p.destroy();
		    	
		    	if(taxiiServerNotResponding) {
		    		status = "Failed";
		    		detailedStatus = "Exception: didn't get a poll response";
		    		log.error(detailedStatus);
		    	}
		    	else if(hasError) {
		    		status = "Failed";
		    		String str = (String)errorAL.get(0);
		    		String detail = "";
		    		
		    		if(str.indexOf("unable to create output directory") >=0) {
		    			detail = "  Verify that stixtransclient.destination.directory is properly configured.";
		    		}
		    		else if(str.indexOf("IOError") >=0) {
		    			detail = "  Verify that stixtransclient.client.basedir is properly configured and readable.";
		    		}

		    		detailedStatus = "Exception: " + str + detail;
		    		log.error(detailedStatus);
		    	}
		    	else if(hasWarning) {
		    		status = "Success";
		    		detailedStatus = "Completed with warnings";
		    	}
		    	else {
		    		status = "Success";
		    		detailedStatus = "Completed sucessfully";
		    	}
	    	}
	    	else {
	    		status = "Failed";
	    		detailedStatus = "Error: Resource Check failed";
	    	}
	    	
	    	log.info("Status of Processing events: " + status);
	    	
	    	if("Success".equals(status)) {
	    		persistTimestamps(validatedProcessType, validatedCollection);
	    	}
	    	else if("Failed".equals(status)){ //Still need to record timestamps, otherwise it loops the failure forever
	    		persistTimestamps(validatedProcessType, validatedCollection);
	    	}
    	}
    	catch(IllegalArgumentException e) {
    		detailedStatus = "Error: Invalid process type: ";
    		log.error(status,e);
    	}
    	catch(IOException e) {
    		detailedStatus = "Error: Processing events failed. IOException";
    		log.error(status,e);
    	}
    	catch(InterruptedException e) {
    		detailedStatus = "Error: Processing events failed. InterruptedException";
    		log.error(status,e);
    	}
    	finally {
    		safeCloseBR(stdError);
    	}
    	
        return new MispTransClient(counter.incrementAndGet(),
        		            status,
        		            detailedStatus,
        		            validatedProcessType,
        		            validatedCollection,
                            this.getBeginTimestamp(),
                            this.getEndTimestamp()
        					);
    }
    
    private String getCommandStr(String processType, String collection)  {
     	List<String> validProcessTypes = new ArrayList<String>();
    	
     	
    	validProcessTypes.add("");
    	validProcessTypes.add("stixToMisp");
    	validProcessTypes.add("xmlOutput");
 
    	String qualifiedPythonCommand = Config.getProperty("bin.filepath") + "/" + Config.getProperty("python.command");
    	String pollUrl = Config.getProperty("stixtransclient.poll.url"); 

    	
    	
    	// flare/taxii client key
    	String clientKey = Config.getProperty("stixtransclient.client.key"); 
    	
    	// flare/taxii client cert
    	String clientCert = Config.getProperty("stixtransclient.client.cert"); 

    	//Source Collection
    	String sourceCollection = collection;
    	
    	//outputType (preserve leading, and trailing spaces)
    	// --xml_output 
    	// --misp 
    	String outputType = " " + Config.getProperty("stixtransclient.output.type.misp") + " "; 
    	
    	String destinationDirectory = Config.getProperty("stixtransclient.destination.directory");
    	String mispUrl = Config.getProperty("stixtransclient.misp.url");
    	String mispKey = EncryptionUtil.decrypt(Config.getProperty("stixtransclient.misp.key"));	


    	
    	setTimestamps(collection,processType);

    	String commandStr = "";

    	if ( !validProcessTypes.contains(processType) )  {
    		throw new IllegalArgumentException("Invalid Process Type: " + processType);
    	}
  
    	if("stixToMisp".equals(processType) ) {
            // Sample stixToMisp command:
    		//
    		// stixtransclient.py 
    		// --poll-url https://10.23.218.172:8443/flare/taxii11/poll/ 
    		// --key FLAREclient1.key 
    		// --cert FLAREclient1.crt 
    		// --taxii --misp --misp-url http://10.23.218.173 --misp-key uOeRefdtdia8oOcGZB9YHhhypidoW9PKMM2oIXZx --collection NCPS_Automated 
    		
    		commandStr = qualifiedPythonCommand
        			+ "  --poll-url " + pollUrl
        			+ " --key " + clientKey
        			+ " --cert " + clientCert 
        			+ " --taxii "
        			+ outputType 
        			+ " --misp-url " + mispUrl
        			+ " --misp-key " + mispKey
        			+ " --collection " + sourceCollection
        			+ " --begin-timestamp " + this.getBeginTimestamp()
        			+ " --end-timestamp " + this.getEndTimestamp()
        			;
    		
    		
        	// --misp_published  (Passing this argument will automatically set the published value to True.
    		//      NOTE: DONT PASS A NAME VALUE PAIR for this argument. Just it's existance is required. 
    		//      Passing a value with this argument will break the python command itself.
        	//      MISP Events wont require approval from User in order to be disseminated to other MISP Servers)
        	String mispPublished = Config.getProperty("stixtransclient.misp.published");
        	if((mispPublished != null)&&(mispPublished.length() > 0))
        	{
        		log.info("mispPublished property detected in configuration. Appending argument to command.");
        		commandStr = commandStr + " --misp-published ";
        	}
        	else
        	{
        		log.info("mispPublished NOT detected in configuration or it's value is emptystring.");
        	}
    		
    	}
    	else if("xmlOutput".equals(processType) ) {
            // Sample xmlOutput command:
    		//
   		    // /usr/local/bin/stixtransclient.py  
    		// --poll-url https://10.23.218.172:8443/flare/taxii11/poll/ 
    		// --key /home/flaredev/flare/FLAREclient1.key 
    		// --cert /home/flaredev/flare/FLAREclient1.crt 
    		// --taxii  
    		// --collection NCPS_Automated 
    		// --xml_output /home/flaredev/flare/stix3
    		
    		outputType = " --xml_output ";
    		
    		commandStr = qualifiedPythonCommand
        			+ "  --poll-url " + pollUrl
        			+ " --key " + clientKey
        			+ " --cert " + clientCert 
        			+ " --taxii " 
        			+ " --collection " + sourceCollection
        			+ " --begin-timestamp " + this.getBeginTimestamp()
        			+ " --end-timestamp " + this.getEndTimestamp()
        			+ outputType + destinationDirectory;        		
        }
    	else {
    		log.info("Unknown processType: " + processType);  
        	commandStr = qualifiedPythonCommand;
    	}
    	log.debug("CTI-Toolkit Raw Command: ");
    	log.debug(commandStr);
    	return commandStr;
    }
    
    
    /***************
    @RequestMapping("/showProperties")
    public String showProperties() {
    	
    	//Properties prop = new Properties();
      	
    	//try(InputStream input = new FileInputStream("config/config.properties");){
    		//prop.load(input);
    	//}
    	//catch(IOException e) {
    		//log.error("Exception occurred in showProperties()",e);
    	//}
    	
    	//String propToString = "<br>";
    	
    	//for(String key : prop.stringPropertyNames()) {
    		  //String value = prop.getProperty(key);
    		  //String str = key + " => " + value;
    		  //propToString += str + "<br>";
    		//}
    	
    	//log.info("Properties: " + propToString);
    	
    	//return "Properties: " + propToString;
    }
    ********************/

    @RequestMapping("/checkTaxiiStatus")
    private HealthCheckResponse checkTaxiiStatus() {
    	int responseCode = 0;
    	String url = "";
        String resourceType="FLARE/TAXII";
    	url = Config.getProperty("stixtransclient.poll.baseurl");	    	
    	
        String cmd="curl -vk " + url;
        
        Process p = null;
        String s = null;
        
        try {
        	//log.info("cmd: " + cmd);
        	p = Runtime.getRuntime().exec(cmd);
        }
        catch(IOException e) {
        	log.error("IOException occurred in checkTaxiiStatus()",e);
        }
       
        if(p != null) {
	    	try (BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));)
	    	{
	             while ((s = stdError.readLine()) != null)
	            {
		    		if(s.indexOf("HTTP/1.1 200 OK") >=0){
		    			responseCode=200;
		    			break;
		    		}
		    		//Connection refused
		    		else if(s.indexOf("Connection refused") >=0){
		    			responseCode=401;
		    			break;
		    		}
		    		//Connection timed out
		    		else if(s.indexOf("Connection timed out") >=0){
		    			responseCode=408;
		    			break;
		    		}
		    		//Connection timed out
		    		else if(s.indexOf("curl: ") ==0){
		    			log.info(s);
		    			break;
		    		}
		    		else {
		    			//log.info(s);
		    		}
	            }	        
	     	}
	    	catch(MalformedURLException e) {
	    		log.error("MalformedURLException occurred in checkTaxiiStatus()",e);
	    	}
	    	catch(IOException e) {
	    		log.error("IOException occurred in checkTaxiiStatus()",e);
	    	}
        }
 
        return new HealthCheckResponse(resourceType, url, responseCode);
    }
      
    @RequestMapping("/checkMispStatus")
    private HealthCheckResponse checkMispStatus() {
    	int responseCode = 0;
    	String url = "";
        String resourceType="Misp";
        	
    	try 
    	{
	    	url = Config.getProperty("stixtransclient.misp.url");
	    	responseCode = getResponseCode(url);
     	}
    	catch(MalformedURLException e) {
    		log.error("MalformedURLException occurred in checkMispStatus()",e);
    	}
    	catch(IOException e) {
    		log.error("IOException occurred in checkMispStatus",e);
    	}
    	catch(URISyntaxException e) {
    		log.error("URISyntaxException occured in checkMispStatus");
    	}
      	
        return new HealthCheckResponse(resourceType, url, responseCode);
    }
    
    @RequestMapping("/refreshConfig")
    public void refreshConfig() {
    	    log.info("Controller refreshConfig - Attempting to Reload Configuration Properties and Update the Service.");
        	log.info("Controller refreshConfig Step 1of5 - Attempt to STOP Quartz Jobs...");
    		stopQuartzJobs();
        	log.info("Controller refreshConfig Step 2of5 - Attempt to STOP Quartz Scheduler...");
    		stopQuartzScheduler();
        	log.info("Controller refreshConfig Step 3of5 - Attempt to reload Configuration Properties...");

        	log.info("Controller refreshConfig Step 4of5 - Reset Begin/End TimeStamps. Will be initialized via Configuration Properties...");
        	beginTimestamp = null;
            endTimestamp = null;
        	
    		Config.loadConfig();
    		log.info("Controller refreshConfig Step 5of5 - Attempt to ReSTART the Quartz Scheduler...");
    		initQuartz();
    }
    
    @RequestMapping("/initQuartz")
    public void initQuartz() {
    	log.info("Controller - Asked to Initialize Quartz Scheduler...");
    	
    	String quartzFrequencyStr = Config.getProperty("mtc.quartz.frequency"); 
    	int quartzFrequency = 2;

    	
    	if (!(quartzFrequencyStr == null  || "".equals(quartzFrequencyStr))) {
    		quartzFrequency = Integer.parseInt(quartzFrequencyStr);
    	}
    	
		try {
			JobDetail job1 = JobBuilder.newJob(InitializeQuartzJob.class).withIdentity("initializeQuartzJob", "group1").build();

			Trigger trigger1 = TriggerBuilder.newTrigger().withIdentity("simpleTrigger", "group1")
					                         .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(quartzFrequency)).build();   
			Scheduler scheduler1 = new StdSchedulerFactory().getScheduler();
			if(!scheduler1.isStarted())
			{
		    	log.info("Controller- Quartz Scheduler has not been started automatically. Or has previously stopped. Starting it now.");
				scheduler1.start(); 
				scheduler1.scheduleJob(job1, trigger1); 
			}
			else
			{
		    	log.warn("Controller- Quartz Scheduler has already been started.");
			}
//			
//			JobDetail job2 = JobBuilder.newJob(ByeJob.class).withIdentity("byeJob", "group2").build();
//			Trigger trigger2 = TriggerBuilder.newTrigger().withIdentity("cronTrigger", "group2")
//					.withSchedule(CronScheduleBuilder.cronSchedule("0/5 * * * * ?")).build();
//			Scheduler scheduler2 = new StdSchedulerFactory().getScheduler();
//			scheduler2.start(); 
//			scheduler2.scheduleJob(job2, trigger2); 
			}
		catch(SchedulerException e){
			log.error("Controller - Exception occurred in initQuartz().",e);
		}
    }

    @RequestMapping("/listQuartzJobs")
    public String listQuartzJobs() {
    	log.info("List Quartz Jobs...");
     	String quartzJobsString = "<br>";
		
    	try{
    		Scheduler scheduler = new StdSchedulerFactory().getScheduler(); 

    		for (String groupName : scheduler.getJobGroupNames()) {

    		     for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

    			  String jobName = jobKey.getName();
    			  String jobGroup = jobKey.getGroup();

    			  //get job's trigger
    			  List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
    			  Date nextFireTime = triggers.get(0).getNextFireTime();

    				log.info("[jobName] : " + jobName + " [groupName] : "
    					+ jobGroup + " - " + nextFireTime);
    				
    				quartzJobsString += 
    						"[jobName] : " + jobName + 
    						" [groupName] : " + jobGroup + 
    						" - " + nextFireTime + "<\br>";

    			  }
    		    }
    	}
       	catch(SchedulerException e) {
       		log.error("SchedulerException",e);
    	}  	
    	
    	log.info("Quartz Jobs:" + quartzJobsString);
    	
    	return "Quartz Jobs:" + quartzJobsString;
 
    }
    
    @RequestMapping("/stopQuartzJobs")
    public void stopQuartzJobs() {
    	
    	try
    	{
    		Scheduler scheduler = new StdSchedulerFactory().getScheduler();		
    		TriggerKey triggerKey = TriggerKey.triggerKey("simpleTrigger", "group1");
    		scheduler.unscheduleJob(triggerKey);
    		log.info("The scheduler stopped the Jobs for group1");
    		//redirect(action: "view", params: params)
    	}
       	catch(SchedulerException e) {
 		   log.error("SchedulerException");
       	}  	
    }
    
    //DONT allow stopQuartzScheduler() to be called via the REST API. 
    //Only used for internal purposes when reloading configuration properties and must insure scheduler updated as well.
    //The scheduler will be terminated when the jvm is terminated. 
    private void stopQuartzScheduler() {
		try
		{
			log.info("Controller - Attempting to STOP the Quartz Scheduler...");
			Scheduler scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.shutdown();
			log.info("Controller - The scheduler is STOPPED successfully.");
		}
	   	catch(SchedulerException e) {
			   log.error("Controller - has encountered a SchedulerException while attempt to Stop the Quartz Scheduler.");
	   	}  	
    }
    
    
    private boolean checkResources() {
    	boolean resourcesAvailable = true;
    	
    	 HealthCheckResponse response = null;
    	 
    	 response = checkTaxiiStatus();
    	 if(response.getStatusCode() == 200) {
    		 log.info("TAXII health check passed."); 
    	 }
    	 else {
    		 log.error("TAXII health check failed."); 
    		 resourcesAvailable = false;
    	 }
    	 
    	 response = checkMispStatus();
    	 if(response.getStatusCode() == 200) {
    		 log.info("Misp health check passed."); 
    	 }
    	 else {
    		 log.error("Misp health check failed.");
    		 resourcesAvailable = false;
    	 }
    	
    	return resourcesAvailable;
    }
    
    private static int getResponseCode(String urlString) throws MalformedURLException, URISyntaxException, IOException {
    	RestTemplate restTemplate = new RestTemplate();
    	URI uri = new URI(urlString);
    	ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, null, String.class);
    	return response.getStatusCodeValue();
    }

    private String getTimestamp() {
    	String formatDateTime =  "";
    	final String dateTimeTemplate = "%sT%s+00:00";
    	final String dateFormat = "yyyy-MM-dd";
    	final String timeFormat = "HH:mm:ss";
    	
    	 //Get current date time
        LocalDateTime now = LocalDateTime.now();
    	
        //Condtruct formatted date
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        String formatedDate = now.format(dateFormatter);

        //Construct formatted time
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);
        String formatedTime = now.format(timeFormatter);
        
        //Construct formatted dateTime
        formatDateTime =  String.format(dateTimeTemplate, formatedDate, formatedTime );
        
        return formatDateTime;
    }
    
    private void setTimestamps(String collection, String processType) {
        final String epoch = "1970-01-01T00:00:00+00:00";

        beginTimestamp = null;
        endTimestamp = null;

        //Refresh each time so that service does not need to be restarted to refresh the conf values
        //refreshConfig();  If persistTimeStamps() is writing to the Config file and then calling LoadConfig to read them back into Memory, dont need Refresh Here!
        
        String latestValFromConfig = Config.getProperty("stixtransclient.poll.endTimestamp" + "." + collection + "." + processType);
        
        beginTimestamp = latestValFromConfig;
        
        String nexTimestamp = getTimestamp();
        endTimestamp = nexTimestamp;
       
        if(beginTimestamp == null || "".equals(beginTimestamp)) {
        	beginTimestamp = epoch;
        }

       //log.info("Collection: " + collection + " Process Type: " + processType + " beginTimestamp: " + beginTimestamp + " endTimestamp: " + endTimestamp);
    }
    
    private String getBeginTimestamp() {
    	return this.beginTimestamp;
    }
    
    private String getEndTimestamp() {
    	return this.endTimestamp;
    }
    
    private boolean suppressWarning(String inStr) {
 	   boolean returnVal=false;
 	   final String pythonWarning = "You're using python 2, it is strongly recommended to use python >=3.5"; 
 	   
 	   if( inStr.indexOf(pythonWarning) >=0) {
 		   returnVal=true;
 	   }

 	   return returnVal;
    }
    
    private void persistTimestamps(String processType, String collection) {
        //Write value of nexTimestamp to the Config file
        //log.info("persistTimestamps " + " beginTimestamp: " + beginTimestamp + " endTimestamp: " + endTimestamp);
  	
        Config.setProperty("stixtransclient.poll.beginTimestamp" + "." + collection + "." + processType, beginTimestamp);
        Config.setProperty("stixtransclient.poll.endTimestamp" + "." + collection + "." + processType, endTimestamp);
    }
    
    private String validateValue(String inStr, String validationProperty) {
    	String retStr="";
		String[] validValues =  Config.getProperty(validationProperty).split(",");
		
		for(int i=0; i<  validValues.length; i++) {
			if(validValues[i].equals(inStr)) {
				retStr = validValues[i];
			}
		}
    	
    	return retStr;
    }
	
	private void safeCloseBR(BufferedReader br) {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
				log.error("safeCloseBR",e);
			}
		}
	}
}

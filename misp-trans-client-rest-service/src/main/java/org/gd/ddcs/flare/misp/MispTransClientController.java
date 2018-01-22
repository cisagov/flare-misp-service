package org.gd.ddcs.flare.misp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.gd.ddcs.flare.misp.Application;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 



import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;



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
    private final Logger log = LoggerFactory.getLogger(MispTransClientController.class);
    
    boolean resourcesAvailable = false;
   
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
    	boolean ioError = false;
    	
    	
    	try {
	    	log.info("Processing events...");
	    	
	    	resourcesAvailable = checkResources();
	    	
	    	if(resourcesAvailable) {
	    		if("".equals(collection)) {
	    			collection = Config.getProperty("stixtransclient.source.collection");	    	    	
	    		}
	    		
	    		log.info("Calling stixtransclient.py" + 
	    		         " with args:" +
	    				 " processType: " + processType +
	    		         " collection: " + collection);
	    		
	    		//Construct command
		    	String cmd = getCommandStr(processType, collection);
		    	log.info("cmd: " + cmd);;

		    	
		    	Process p = Runtime.getRuntime().exec(cmd);
	            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	
	            // Read the output from the command
	            String s = null;
	            
	            while ((s = stdInput.readLine()) != null)
	            {
		    		log.info(s);
	            }
	            
	           // Read any errors from the attempted command
	           while ((s = stdError.readLine()) != null)
	           {
	        	    if (s.equals("Exception: didn't get a poll response")) {
		    			errorAL.add(s);
			    		log.error(s);
			    		taxiiServerNotResponding = true;
		                hasError = true;
	        	    }
//	        	    else if (s.equals("IOError: [Errno 13] Permission denied")) {
//		    			errorAL.add(s);
//			    		log.error(s);
//			    		ioError = true;
//		                hasError = true;
//	        	    }
	        	    //else if ( s.toLowerCase().indexOf("error") >=0)
		        	else if ( s.indexOf("ERROR") >=0 ||
		        			  s.indexOf("Error") >=0)
	        	    {
		        	    log.info("Ksc Error String: " +  s);
		        		
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
		    			warningAL.add(s);
			    		log.info("WARNING: " + s);
		                hasWarning = true;
		    		}
	           }
		    	
		    	p.waitFor();
		    	p.destroy();
		    	
		    	if(taxiiServerNotResponding) {
		    		detailedStatus = "Exception: didn't get a poll response";
		    	}
		    	if(ioError) {
		        	String destinationDirectory = Config.getProperty("stixtransclient.destination.directory");

		    		detailedStatus = "Exception: IO Error.  Check log file.";
		    		
		    		//log.error
		    		//stixtransclient.client.basedir
		    	}
		    	else if(hasError) {
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
	    		detailedStatus = "Resource Check falied";
	    	}
	    	
	    	log.info("Processing events: " + status);

    	}
    	catch(IllegalArgumentException e) {
    		e.printStackTrace();
    		detailedStatus = "Error: Invalid process type: ";
    		log.info(status);
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		detailedStatus = "Error: Processing events failed.";
    		log.info(status);
    	}
    	
        return new MispTransClient(counter.incrementAndGet(),
        		            status,
        		            detailedStatus,
                            processType,
                            collection);
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
    	String mispKey = Config.getProperty("stixtransclient.misp.key");	
    	
    	String beginTimeStamp = getBeginTimestamp(collection,processType);
    	
 
    	String commandStr = "";
    	
    	log.info("processType: " + processType); 

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
        			+ " --begin-timestamp " + beginTimeStamp
        			;    		
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
        			+ " --begin-timestamp " + beginTimeStamp
        			+ outputType + destinationDirectory;        		
        }
    	else {
    		log.info("Unknown processType: " + processType);  
        	commandStr = qualifiedPythonCommand;
    	}
   	
    	return commandStr;
    }
    
    @RequestMapping("/properties")
    public String props() {
    	
    	Properties prop = new Properties();
    	InputStream  input = null;
      	
    	try {
    		input = new FileInputStream("config/config.properties");
    		prop.load(input);
    	}
    	catch(IOException ex) {
    		ex.printStackTrace();
    	}
    	
    	if(input != null) {
    		log.info("inputStream is not null");
    	}
    	else {
    		log.info("inputStream is null");
    	}
    	
    	String propToString = "<br>";
    	
    	for(String key : prop.stringPropertyNames()) {
    		  String value = prop.getProperty(key);
    		  String str = key + " => " + value;
    		  propToString += str + "<br>";
    		}
    	
    	log.info("Properties: " + propToString);
    	
    	return "Properties: " + propToString;
    }

    @RequestMapping("/checkTaxiiStatus")
    public HealthCheckResponse checkTaxiiStatus() {
    	int responseCode = 0;
    	String url = "";
        String resourceType="FLARE/TAXII";

        String statusTemplate = "Process Events: Process Type - %s %s ";
       
    	try 
    	{
	    	log.info("calling disableSslVerification()");
	    	disableSslVerification();
	    	log.info("returned from calling disableSslVerification()");
    	
	    	url = Config.getProperty("stixtransclient.poll.baseurl");
	    	responseCode = getResponseCode(url);
	    	log.info("checkResources() url: " + url + " response: " + responseCode);
     	}
    	catch(Exception ex) {
    		ex.printStackTrace();
    	}
      	
        return new HealthCheckResponse(resourceType, url, responseCode);
    }
    
    @RequestMapping("/checkMispStatus")
    public HealthCheckResponse checkMispStatus() {
    	int responseCode = 0;
    	String url = "";
        String statusTemplate = "Process Events: Process Type - %s %s ";
        String resourceType="Misp";
        	
    	try 
    	{
	    	url = Config.getProperty("stixtransclient.misp.url");
	    	responseCode = getResponseCode(url);
	    	log.info("checkResources() url: " + url + " response: " + responseCode);
     	}
    	catch(Exception ex) {
    		ex.printStackTrace();
    	}
      	
        return new HealthCheckResponse(resourceType, url, responseCode);
    }
    
    @RequestMapping("/refreshConfig")
    public void refreshConfig() {
    	Config.loadConfig();
    }
    
    public boolean checkResources() {
    	int responseCode = 0;
    	String url = "";
    	boolean resourcesAvailable = true;
    	
    	try 
    	{
	    	 HealthCheckResponse response = null;
	    	 
	    	 response = checkTaxiiStatus();
	    	 if(response.getStatusCode() == 200) {
	    		 log.info("TAXII health check passed."); 
	    	 }
	    	 else {
	    		 log.info("TAXII health check failed."); 
	    		 resourcesAvailable = false;
	    	 }
	    	 
	    	 response = checkMispStatus();
	    	 if(response.getStatusCode() == 200) {
	    		 log.info("Misp health check passed."); 
	    	 }
	    	 else {
	    		 log.info("Misp health check failed.");
	    		 resourcesAvailable = false;
	    	 }
    	}
    	catch(Exception ex) {
    		ex.printStackTrace();
   		    resourcesAvailable = false;
    	}
    	
    	return resourcesAvailable;
    }
    
    public static int getResponseCode(String urlString) throws MalformedURLException, IOException {
    	URL u = new URL(urlString); 
    	HttpURLConnection huc =  (HttpURLConnection)  u.openConnection(); 
    	huc.setRequestMethod("GET"); 
    	huc.connect(); 
    	return huc.getResponseCode();
    }

    private static void disableSslVerification() {
    	try
    	{
    		// Create a trust manager that does not validate certificate chains
    		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
    			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    				return null;
    			}
            
    			public void checkClientTrusted(X509Certificate[] certs, String authType) {
    			}
    			public void checkServerTrusted(X509Certificate[] certs, String authType) {
    			}
    		}};

    		// Install the all-trusting trust manager
    		SSLContext sc = SSLContext.getInstance("SSL");
    		sc.init(null, trustAllCerts, new java.security.SecureRandom());
    		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    		// Create all-trusting host name verifier
    		HostnameVerifier allHostsValid = new HostnameVerifier() {
    			public boolean verify(String hostname, SSLSession session) {
    				return true;
    			}
    		};

        	// Install the all-trusting host verifier
        	HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    	} catch (NoSuchAlgorithmException e) {
        	e.printStackTrace();
    	} catch (KeyManagementException e) {
        	e.printStackTrace();
    	}
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
    
    private String getBeginTimestamp(String collection, String processType) {
        final String epoch = "1970-01-01T00:00:00+00:00";
        String beginTimestamp = Config.getProperty("stixtransclient.poll.timestamp" + "." + collection + "." + processType);
        String nexTimestamp = getTimestamp();
       
        if(beginTimestamp == null || "".equals(beginTimestamp)) {
        	beginTimestamp = epoch;
        }
        
        log.info("Collection: " + collection + " Process Type: " + processType + " beginTimestamp: " + beginTimestamp);
        log.info("Collection: " + collection + " Process Type: " + processType + " nexTimestamp: " + nexTimestamp);
        
        //Write value of nexTimestamp to the Config file
        Config.setProperty("stixtransclient.poll.timestamp" + "." + collection + "." + processType, nexTimestamp);
        
        return beginTimestamp;
   }
}

/*
 * 

Misp
10.23.218.173
admin@admin.test
12qwaszx@#WESDXC

 * 
 */


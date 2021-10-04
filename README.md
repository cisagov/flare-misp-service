# FLARE MISP Service


This service is provided to enable the specific use case of retrieving AIS data (in STIX 1.1.1 format) from AIS and loading the content in a MISP Server.  For more information about DHS and establishing access to the AIS service, please see https://us-cert.gov/ais.

The Flare MISP Service has been designed to provide a mechanism to routinely poll content from a TAXII server and make it available in a MISP server.  This service provides transformation of STIX content to MISP and to provide the TAXII server functionality.  It can be configured to pull from a TAXII server and convert the STIX files into MISP format or just download the STIX as xml at regular intervals. 


## Table of Contents
1. [About FLARE MISP Service](#about)
2. [Starting FLARE MISP Service](#starting)
3. [Endpoints](#endpoints)
4. [Building Tar Ball](#packaging)
5. [Installation](#installation)
   
# <a name="about"></a>About FLARE MISP Service

FlareMispService is basically a REST Web Service

#### Web Service’s responsibilities:
* Run a Scheduler to wake up tasks.
* Read the configuration file and pass values as args to this applicaiton.
* Provide a basic REST API.
    * Reload configurable properties from file system
    * Check TAXI connectivity
    * Check MISP connectivity
    * Manually start a Poll for work to do (outputs XML only)
    * Manually start a Poll for work to do (outputs MISP Event to MISP Server)
    * Handle request from Scheduler to do work (MISP).
    * Restart Scheduler (assuming it crashed or failed auto startup)
    * Display list of Quartz Jobs (only 1 recurring job unless you program more)
    * Stop all Quartz Jobs 

# <a name="starting"></a>Starting FLARE MISP Service
#### To start the FlareMispService:
Run the shell script:

    cd /opt/mtc
    /opt/mtc/runFLAREmispService.sh

Output should display on the console. 
Note that the version in the script may need to be updated.

#### Independently verify the service is running with the following command:
   	ps -ef | grep mtc-rest-service

#### To verify the service can connect to the FlareSuite’s TAXII Server:
    a.	https://localhost:8443/checkTaxiiStatus
    b.	Should seem something similar to: {"resourceType":"FLARE/TAXII","resource":"https://10.23.218.172:8443","statusCode":200}
    c.	The statusCode of value 200 indicates a successful connection to the TAXII server
    d.	If the value is not 200, verify the ip address and port in step 6e and 6f above.
    e.	Otherwise must verify that the FlareSuite TAXII server is running and accessible to the server where FlareMispService is running.

#### To verify the service can connect to the MISP Server:
    a.	https://localhost:8443/checkMispStatus
    b.	Should seem something similar to: {"resourceType":"Misp","resource":"http://10.23.218.173","statusCode":200}
    c.	The statusCode of value 200 indicates a successful connection to the TAXII server
    d.	If the value is not 200, verify the ip address in step 6g above. (no port specified because MISP uses a predefined port #)
    e.	Otherwise must verify that the MISP server is running and accessible to the server where FlareMispService is running.


#### Quartz output messages:
When it’s time for the Quartz job to poll the FlareSuite TAXI server for STIXX information, you can expect to see the following information on the command line and/or in /opt/mtc/logs/mtc-rest-service.log
(the dates and times will change accordingly)

    2018-02-28 09:36:46 - Processing events...
    2018-02-28 09:36:46 - TAXII health check passed.
    2018-02-28 09:36:47 - Misp health check passed.
    2018-02-28 09:36:48 - Processing events: Success
    2018-02-28 09:36:48 - {"id":14,"status":"Success","detailedStatus":"Completed sucessfully","processType":"xmlOutput","collection":"MISP","beginTimestamp":"2018-02-28T09:35:47+00:00","endTimeStamp":"2018-02-28T09:36:47+00:00"}

# <a name="endpoints"></a> Endpoints
### Verify FLARE MISP Service is running with the following endpoints:

####  Verify the Quartz Scheduler is Up and Running
URL:  https://localhost:8443/listQuartzJobs
﻿Quartz Jobs:[jobName] : initializeQuartzJob [groupName] : group1 - Thu Mar 22 12:30:25 EDT 2018<r>


####  Verify connectivity to MISP Server
URL: https://localhost:8443/checkMispStatus
﻿{"resourceType":"Misp","resource":"(MISP URL here)","statusCode":200}


####  Verify connectivity to FLARE TAXII Server
URL: https://localhost:8443/checkTaxiiStatus
﻿{"resourceType":"FLARE/TAXII","resource":"(MISP URL here):(MISP Port here)","statusCode":200}


####  Verify the configuration property file can be loaded/reloaded
This does not refresh the timestamps
URL: https://localhost:8443/refreshConfig
{"Controller":"/refreshConfig:","Step 1 of 5":"Attempt to STOP Quartz Jobs...","Step 2 of 5":"Attempt to STOP Quartz Scheduler...","Step 3 of 5":"Attempt to reload Configuration Properties...","Step 4 of 5":"Reset Begin/End TimeStamps. Will be initialized via Configuration Properties...","Step 5 of 5":"Attempt to ReSTART the Quartz Scheduler..."}

### Verify Connectivity with the following endpoints:

####  Connect to FLARE TAXI, download any new STIX, then save as XML to temporary directory  “/opt/mtc/out”
URL: https://localhost:8443/misptransclient?processType=xmlOutput
Method:POST

Result:
﻿{"id":5,"status":"Success","detailedStatus":"Completed sucessfully","processType":"xmlOutput","collection":"MISP","beginTimestamp":"2018-03-22T11:10:56+00:00","endTimeStamp":"2018-03-22T12:33:05+00:00"}


####  Connect to FLARE TAXI, convert new STIX to MISP, then upload to MISP Server
URL: https://localhost:8443/misptransclient?processType=stixToMisp
Method:POST

Result:
﻿{"id":7,"status":"Success","detailedStatus":"Completed sucessfully","processType":"stixToMisp","collection":"MISP","beginTimestamp":"2018-03-22T12:33:23+00:00","endTimeStamp":"2018-03-22T12:34:18+00:00"}


#### Login to MISP Server to verify new MISP Events show up
URL: http://<MISP URL>/users/login
User:  admin@admin.test

Note: The FLARE-MISP-SERVICE software component does not modifies the content of the STIX files from 
      a TAXII 1.1.1 server before forwarding (via an HTTP Post) to a MISP Server and does not set any publication status in MISP Server.

# <a name="packaging"></a>To build the tarball:

1) Pull the most recent code 

   Set up Maven and Java 8 properly
   
2) go to the ~git/FLAREmispService/misp-trans-client-rest-service folder. 

   There should be a pom.xml file in this directory. Run command:
   
		mvn clean package
				
   in order to build the package
   
3) run the packageDeployment.sh script to pack the built mvn package into a tarball the tarball should be in ~git/FLAREmispService/misp-trans-client-rest-service/deploy/ named FLAREmispService.tar
     
		./packageDeployment.sh

# <a name="installation"></a>Installation:

Refer to the SAG for more details

#### 1) Create the directory /opt/mtc and set its permissions:

```sudo mkdir /opt/mtc```

```sudo chmod 755 /opt/mtc```

#### 2) Copy the FLAREmispService.tar to /opt/mtc:

```sudo cp FLAREmispService.tar /opt/mtc```

#### 3)	Untar the tarball.

```sudo tar –xvf FLAREmispService.tar```

It _should_ contain the directories:

    /opt/mtc
    /opt/mtc/avro
    /opt/mtc/out
    /opt/mtc/logs
    /opt/mtc/config

The tarball should contain 2 properties files in /opt/confg:

    application.properties
    config.properties

The /logs and /out directories should be empty (initially).


#### 4)	Verify the ownership of the subdirectories under /opt/mtc are owned by ‘flare’  (or ‘flaredev’)
	If the directories are not owned by the proper account, change them.
        a)	cd /opt/mtc
        b)	sudo chown flare:flare /opt/mtc/*

#### 5)	Copy certificate and key for the TAXII Server (FlareSuite) to /opt/mtc/config

    a. The names and locations of the key and cert files are configurable with properties in  /opt/mtc/config/config.properties
    b. They default to the location /opt/mtc/config
    c. The key and cert files are specific to the FlareSuite TAXII Server

#### 6)	Adjust remaining /opt/mtc/config/config.properties file values for the Deployed Environment
    Modify property: bin.filepath to stixtransclient.py location
    a.	Modify property: stixtransclient.client.key to be equal to path and name of the key file from step #5
    b. 	Modify property: stixtransclient.client.cert to be equal to path and name of the certificate file from step #5
    c.	Modify property: stixtransclient.poll.baseurl to have the IP and port for the Flaresuite TAXII server in deployed environment
    d.	Modify property: stixtransclient.poll.url 
        i.	Start with the same value as stixtransclient.poll.baseurl property in step (6c)
        ii.	append the following: /flare/taxii11/poll/
    e.	Modify property: stixtransclient.misp.url to have the IP for the MISP Server.
	
#### 7)	Tweak the /opt/mtc/config/application.properties file to appropriate values for the Deployed Environment.
    a.  Create a new P12 keystore using both your client certificate and private key

          openssl pkcs12 -export -in client.crt -inkey client.key -name client -out clientkeystore.p12

    b.  Modify SSL Keystore as follows
                
          client.ssl.key-store=file:config/clientkeystore.p12

    c.  Ensure Two-way SSL is set to true

          2way.ssl.auth=true

    d.	Verify the logging property: logging.level.root = INFO  (can be WARN, INFO, or DEBUG for additional information)
    
   
#### 8) Start the service
See [Starting FLARE MISP Service](#starting)

#### 9) Stix Data Tracking
     a. Stix packages downloaded or uploaded to MISP server are tracked using Apache Avro data serialization system.
     b. https://avro.apache.org/
     c. Tracking data file is located under /opt/mtc/avro
     
#### 10) Ability to filter STIX 1.1.1:
flare-misp-service does not perform any filtering on the packages that come from a TAXII 1.1.1 server (i.e., hailataxi.com).
One possible option is to get the packages and store them locally using `xmlOutput` process type. Then, write a script
that process and filter the content from these XML files before posting them to the MISP server.

#### 11) Ability to run with multiple collection
flare-misp-service does not support the pulling of STIX packages from a TAXII 1.1.1 server using more than one (1) collection.
One possible option is to run multiple flare-misp-client server where each pull a packages from a different collection than 
the other flare-misp-service server. It is therefore feasible to run multiple flare-misp-services for use with multiple TAXII 
1.1.1 servers and MISP servers.

#### 12) Additional Notes on Testing FLARE-MISP-SERVICE:

It is important to note that the FLARE-MISP-SERVICE software component does not in anyway modifies the content of the STIX files from 
a TAXII 1.1.1 server before forwarding (via an HTTP Post) to a MISP Server. Furthermore, FLARE-MISP-SERVICE uses STIX ID from the XML Package
to avoid sending duplicates to the MISP Server.

We've encountered the following issues. We've documented these findings in our internal issue tacking system (# 2836)

- An issue with files that have detected PII. Since PII detected files are being mediated by Human Review component (via a person), this causes the 
file to be duplicated after a remediation. Thus, only one file will be forwarded to the MISP Server.
- Observables with IP addresses are not being processed by MISP Server consistently.
- MISP Server rejects some STIX files from TAXII 1.1. Manually submissions of the rejected files are also rejected. Thus, we believe
it is an issue with MISP Server.
